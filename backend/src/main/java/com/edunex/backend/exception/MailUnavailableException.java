package com.edunex.backend.exception;

/** Answered as 503 by the existing handler of AiUnavailableException. */
public class MailUnavailableException extends AiUnavailableException {
    public MailUnavailableException(String message) {
        super(message);
    }
}