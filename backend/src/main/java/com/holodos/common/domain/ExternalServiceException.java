package com.holodos.common.domain;

/** A call to an external integration (Google Keep / Tasks) failed. Mapped to 502. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException(String message) {
        super(message);
    }
}
