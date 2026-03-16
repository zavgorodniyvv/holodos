package com.holodos.media.application;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.holodos.media.infrastructure.FilesystemMediaStorageGateway;
import com.holodos.media.infrastructure.MediaStorageConfig;
import com.holodos.media.infrastructure.MediaStorageProperties;
import com.holodos.media.infrastructure.S3MediaStorageGateway;
import org.junit.jupiter.api.Test;

class MediaStorageConfigTest {

    private final MediaStorageConfig config = new MediaStorageConfig();

    @Test
    void createsFilesystemGatewayWhenProviderIsFilesystem() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setProvider(MediaStorageProperties.Provider.FILESYSTEM);
        properties.setRootPath("./data/media");

        var gateway = config.mediaStorageGateway(properties);
        assertInstanceOf(FilesystemMediaStorageGateway.class, gateway);
    }

    @Test
    void createsS3GatewayWhenProviderIsS3() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setProvider(MediaStorageProperties.Provider.S3);
        properties.getS3().setBucket("bucket");
        properties.getS3().setAccessKey("key");
        properties.getS3().setSecretKey("secret");
        properties.getS3().setRegion("us-east-1");
        properties.getS3().setEndpoint("http://localhost:9000");

        var gateway = config.mediaStorageGateway(properties);
        assertInstanceOf(S3MediaStorageGateway.class, gateway);
    }
}
