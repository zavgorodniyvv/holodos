package com.holodos.media.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.media.domain.MediaObject;
import com.holodos.media.infrastructure.MediaObjectRepository;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    MediaObjectRepository mediaObjectRepository;

    @Mock
    MediaStorageGateway mediaStorageGateway;

    @InjectMocks
    MediaService mediaService;

    @Test
    void uploadProductPhotoStoresFileAndUpdatesProduct() throws IOException {
        Product product = new Product();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(mediaStorageGateway.providerName()).thenReturn("FILESYSTEM");
        when(mediaObjectRepository.save(any(MediaObject.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "soap.jpg", "image/jpeg", new byte[] {1, 2, 3});
        MediaObject saved = mediaService.uploadProductPhoto(10L, file);

        assertEquals("image/jpeg", saved.getContentType());
        assertEquals(3L, saved.getSizeBytes());
        assertEquals(saved.getObjectKey(), product.getPhotoKey());
        verify(mediaStorageGateway).store(saved.getObjectKey(), new byte[] {1, 2, 3});
    }

    @Test
    void removeProductPhotoClearsPhotoKeyAndDeletesObject() throws IOException {
        Product product = new Product();
        product.setPhotoKey("products/10/photo.jpg");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        mediaService.removeProductPhoto(10L);

        assertNull(product.getPhotoKey());
        verify(mediaStorageGateway).delete("products/10/photo.jpg");
        verify(mediaObjectRepository).deleteByObjectKey("products/10/photo.jpg");
    }

    @Test
    void getProductPhotoReturnsBytesFromStorage() throws IOException {
        Product product = new Product();
        product.setPhotoKey("products/10/photo.jpg");

        MediaObject media = new MediaObject();
        media.setObjectKey("products/10/photo.jpg");
        media.setContentType("image/jpeg");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(mediaObjectRepository.findByObjectKey("products/10/photo.jpg")).thenReturn(Optional.of(media));
        when(mediaStorageGateway.read("products/10/photo.jpg")).thenReturn(new byte[] {4, 5});

        var payload = mediaService.getProductPhoto(10L);
        assertEquals("image/jpeg", payload.contentType());
        assertArrayEquals(new byte[] {4, 5}, payload.content());
    }
}
