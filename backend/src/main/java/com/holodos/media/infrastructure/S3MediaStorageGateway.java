package com.holodos.media.infrastructure;

import com.holodos.media.application.MediaStorageGateway;
import java.io.IOException;
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3MediaStorageGateway implements MediaStorageGateway {

    private static final String PROVIDER = "S3";
    private final String bucket;
    private final S3Client s3Client;

    public S3MediaStorageGateway(MediaStorageProperties properties) {
        MediaStorageProperties.S3 s3 = properties.s3();
        if (s3 == null) {
            throw new IllegalStateException("holodos.media.s3.* configuration is required when provider=S3");
        }
        if (s3.bucket() == null || s3.bucket().isBlank()) {
            throw new IllegalStateException("holodos.media.s3.bucket is required when provider=S3");
        }
        if (s3.accessKey() == null || s3.secretKey() == null) {
            throw new IllegalStateException("holodos.media.s3.access-key and secret-key are required when provider=S3");
        }

        S3ClientBuilder builder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                s3.accessKey(),
                s3.secretKey()
            )))
            .region(Region.of(s3.region()))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.pathStyleAccess()).build());

        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(s3.endpoint()));
        }

        this.bucket = s3.bucket();
        this.s3Client = builder.build();
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public void store(String objectKey, byte[] content) throws IOException {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (Exception e) {
            throw new IOException("S3 putObject failed", e);
        }
    }

    @Override
    public byte[] read(String objectKey) throws IOException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (Exception e) {
            throw new IOException("S3 getObject failed", e);
        }
    }

    @Override
    public void delete(String objectKey) throws IOException {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            throw new IOException("S3 deleteObject failed", e);
        }
    }
}
