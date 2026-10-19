package com.holodos.media.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.media.domain.MediaObject;
import com.holodos.media.infrastructure.MediaObjectRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    /** Raster formats only: SVG can carry script and would be served from the app origin. */
    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/heic", "image/heif");

    private final ProductRepository productRepository;
    private final MediaObjectRepository mediaObjectRepository;
    private final MediaStorageGateway mediaStorageGateway;

    public MediaService(
        ProductRepository productRepository,
        MediaObjectRepository mediaObjectRepository,
        MediaStorageGateway mediaStorageGateway
    ) {
        this.productRepository = productRepository;
        this.mediaObjectRepository = mediaObjectRepository;
        this.mediaStorageGateway = mediaStorageGateway;
    }

    @Transactional
    public MediaObject uploadProductPhoto(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        String contentType = file.getContentType() == null ? null : file.getContentType().toLowerCase(Locale.ROOT);
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, WebP or HEIC images are supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        String oldObjectKey = product.getPhotoKey();
        String objectKey = buildObjectKey(productId, file.getOriginalFilename());
        try {
            mediaStorageGateway.store(objectKey, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store media object", e);
        }
        // If the database part fails, don't leave the freshly stored file orphaned
        onRollback(() -> deleteStoredObject(objectKey));

        MediaObject mediaObject = new MediaObject();
        mediaObject.setObjectKey(objectKey);
        mediaObject.setContentType(contentType);
        mediaObject.setOriginalFilename(file.getOriginalFilename());
        mediaObject.setSizeBytes(bytes.length);
        mediaObject.setStorageProvider(mediaStorageGateway.providerName());

        MediaObject saved = mediaObjectRepository.save(mediaObject);
        product.setPhotoKey(objectKey);
        productRepository.save(product);

        if (oldObjectKey != null && !oldObjectKey.equals(objectKey)) {
            deleteObjectByKey(oldObjectKey);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public MediaPayload getProductPhoto(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getPhotoKey() == null) {
            throw new IllegalArgumentException("Product photo is not set: " + productId);
        }

        MediaObject mediaObject = mediaObjectRepository.findByObjectKey(product.getPhotoKey())
            .orElseThrow(() -> new IllegalArgumentException("Media object not found for product: " + productId));

        try {
            byte[] bytes = mediaStorageGateway.read(mediaObject.getObjectKey());
            // Objects uploaded before the allow-list (e.g. SVG) are served as opaque binary
            String contentType = ALLOWED_CONTENT_TYPES.contains(mediaObject.getContentType())
                ? mediaObject.getContentType() : "application/octet-stream";
            return new MediaPayload(contentType, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read media object", e);
        }
    }

    @Transactional
    public void removeProductPhoto(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getPhotoKey() == null) {
            return;
        }

        String objectKey = product.getPhotoKey();
        product.setPhotoKey(null);
        productRepository.save(product);
        deleteObjectByKey(objectKey);
    }

    private void deleteObjectByKey(String objectKey) {
        mediaObjectRepository.deleteByObjectKey(objectKey);
        // The file goes only once the database change is committed, otherwise a rollback would leave a dangling key
        afterCommit(() -> deleteStoredObject(objectKey));
    }

    private void deleteStoredObject(String objectKey) {
        try {
            mediaStorageGateway.delete(objectKey);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to delete media object {}", objectKey, e);
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private static void onRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }

    private String buildObjectKey(Long productId, String originalFilename) {
        String sanitized = originalFilename == null ? "image" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "products/%d/%s-%s".formatted(productId, UUID.randomUUID(), sanitized);
    }

    public record MediaPayload(String contentType, byte[] content) {
    }
}
