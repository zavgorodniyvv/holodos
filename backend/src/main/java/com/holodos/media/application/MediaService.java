package com.holodos.media.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.media.domain.MediaObject;
import com.holodos.media.infrastructure.MediaObjectRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

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

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are supported");
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

    private String buildObjectKey(Long productId, String originalFilename) {
        String sanitized = originalFilename == null ? "image" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "products/%d/%s-%s".formatted(productId, UUID.randomUUID(), sanitized);
    }

    public record MediaPayload(String contentType, byte[] content) {
    }
}
