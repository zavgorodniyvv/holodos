package com.holodos.media.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.media.domain.MediaObject;
import com.holodos.media.infrastructure.MediaObjectRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

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

        String contentType = normalizeAndValidateContentType(file.getContentType());

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        if (bytes.length == 0) {
            throw new IllegalArgumentException("Uploaded image is empty");
        }
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Uploaded image exceeds maximum size of 10MB");
        }

        String oldObjectKey = product.getPhotoKey();
        String objectKey = buildObjectKey(productId, file.getOriginalFilename());
        try {
            mediaStorageGateway.store(objectKey, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store media object", e);
        }

        try {
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
        } catch (RuntimeException e) {
            try {
                mediaStorageGateway.delete(objectKey);
            } catch (IOException ignored) {
                // ignore cleanup failure and rethrow original error
            }
            throw e;
        }
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
            return new MediaPayload(mediaObject.getContentType(), bytes);
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
        try {
            mediaStorageGateway.delete(objectKey);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete media object", e);
        }
        mediaObjectRepository.deleteByObjectKey(objectKey);
    }

    private String normalizeAndValidateContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Image content type is required");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are supported");
        }
        return normalized;
    }

    private String buildObjectKey(Long productId, String originalFilename) {
        String sanitized = originalFilename == null ? "image" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "products/%d/%s-%s".formatted(productId, UUID.randomUUID(), sanitized);
    }

    public record MediaPayload(String contentType, byte[] content) {
    }
}
