package com.holodos.media.infrastructure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemMediaStorageGatewayTest {
    @TempDir Path root;
    @TempDir Path outside;

    private FilesystemMediaStorageGateway gateway() {
        return new FilesystemMediaStorageGateway(new MediaStorageProperties(null, root.toString(), null));
    }

    @Test
    void storesAndReadsInsideRoot() throws Exception {
        gateway().store("products/1/photo.jpg", new byte[] {1, 2});

        assertArrayEquals(new byte[] {1, 2}, gateway().read("products/1/photo.jpg"));
    }

    @Test
    void absoluteKeyCannotDeleteFilesOutsideRoot() throws Exception {
        Path victim = Files.writeString(outside.resolve("app.jar"), "important");

        assertThrows(IllegalArgumentException.class, () -> gateway().delete(victim.toString()));
        assertTrue(Files.exists(victim));
    }

    @Test
    void parentSegmentsCannotEscapeRoot() throws Exception {
        Path victim = Files.writeString(outside.resolve("secret.txt"), "secret");
        String key = "../" + outside.getFileName() + "/secret.txt";

        assertThrows(IllegalArgumentException.class, () -> gateway().read(key));
        assertThrows(IllegalArgumentException.class, () -> gateway().delete(key));
        assertTrue(Files.exists(victim));
    }
}
