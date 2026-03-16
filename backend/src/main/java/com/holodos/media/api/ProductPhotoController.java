package com.holodos.media.api;

import com.holodos.media.application.MediaService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/{productId}/photo")
@Validated
public class ProductPhotoController {

    private final MediaService mediaService;

    public ProductPhotoController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductPhotoResponse> upload(
        @PathVariable Long productId,
        @RequestParam("file") @NotNull MultipartFile file
    ) {
        var media = mediaService.uploadProductPhoto(productId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProductPhotoResponse(
            media.getObjectKey(),
            media.getContentType(),
            media.getSizeBytes()
        ));
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable Long productId) {
        var payload = mediaService.getProductPhoto(productId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(payload.contentType()));
        headers.setCacheControl(CacheControl.noCache());
        headers.setContentDisposition(ContentDisposition.inline().filename("product-photo").build());
        return new ResponseEntity<>(payload.content(), headers, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@PathVariable Long productId) {
        mediaService.removeProductPhoto(productId);
        return ResponseEntity.noContent().build();
    }

    public record ProductPhotoResponse(String objectKey, String contentType, long sizeBytes) {
    }
}
