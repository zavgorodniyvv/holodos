package com.holodos.media.application;

import java.io.IOException;

public interface MediaStorageGateway {

    String providerName();

    void store(String objectKey, byte[] content) throws IOException;

    byte[] read(String objectKey) throws IOException;

    void delete(String objectKey) throws IOException;
}
