package com.holodos.media.infrastructure;

import com.holodos.media.application.MediaStorageGateway;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class FilesystemMediaStorageGateway implements MediaStorageGateway {

    private static final String PROVIDER = "FILESYSTEM";
    private final Path rootPath;

    public FilesystemMediaStorageGateway(MediaStorageProperties properties) {
        this.rootPath = Path.of(properties.rootPath()).toAbsolutePath().normalize();
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public void store(String objectKey, byte[] content) throws IOException {
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    @Override
    public byte[] read(String objectKey) throws IOException {
        Path target = resolve(objectKey);
        return Files.readAllBytes(target);
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Path target = resolve(objectKey);
        Files.deleteIfExists(target);
    }

    /** Resolves a key inside the media root; absolute keys and ".." segments must not escape it. */
    private Path resolve(String objectKey) {
        Path target = rootPath.resolve(objectKey).normalize();
        if (!target.startsWith(rootPath) || target.equals(rootPath)) {
            throw new IllegalArgumentException("Invalid media object key");
        }
        return target;
    }
}
