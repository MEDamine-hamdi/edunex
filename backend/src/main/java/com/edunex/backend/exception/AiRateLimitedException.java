package com.edunex.backend.exception;

public class AiRateLimitedException extends AiUnavailableException {

    private final int retryAfterSeconds;

    public AiRateLimitedException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}