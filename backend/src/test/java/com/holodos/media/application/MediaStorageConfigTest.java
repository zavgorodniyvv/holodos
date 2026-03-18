package com.holodos.media.application;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.holodos.media.infrastructure.FilesystemMediaStorageGateway;
import com.holodos.media.infrastructure.MediaStorageConfig;
import com.holodos.media.infrastructure.MediaStorageProperties;
import com.holodos.media.infrastructure.MediaStorageProperties.S3;
import com.holodos.media.infrastructure.MediaStorageProperties.Provider;
import com.holodos.media.infrastructure.S3MediaStorageGateway;
import org.junit.jupiter.api.Test;

class MediaStorageConfigTest {

    private final MediaStorageConfig config = new MediaStorageConfig();

    @Test
    void createsFilesystemGatewayWhenProviderIsFilesystem() {
        MediaStorageProperties properties = new MediaStorageProperties(Provider.FILESYSTEM, "./data/media", null);
        var gateway = config.mediaStorageGateway(properties);
        assertInstanceOf(FilesystemMediaStorageGateway.class, gateway);
    }

    @Test
    void createsS3GatewayWhenProviderIsS3() {
        S3 s3Props = new S3("bucket", "key", "secret", "us-east-1", "http://localhost:9000", true);
        MediaStorageProperties properties = new MediaStorageProperties(Provider.S3, null, s3Props);
        var gateway = config.mediaStorageGateway(properties);
        assertInstanceOf(S3MediaStorageGateway.class, gateway);
    }
}
