package com.holodos.common.domain;

/** Operation is valid in form but conflicts with the current state (e.g. item already purchased). Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
