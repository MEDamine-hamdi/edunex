package com.edunex.backend.exception;

public class TemporaryPasswordExpiredException extends RuntimeException {
    public TemporaryPasswordExpiredException() {
        super("TEMPORARY_PASSWORD_EXPIRED");
    }
}