package com.alfano.gathorapp.chat;

/**
 * Exception thrown when attempting to perform operations on an inactive chat.
 * This exception will be mapped to HTTP 400 status by the
 * GlobalExceptionHandler.
 */
public class InactiveChatException extends RuntimeException {

    public InactiveChatException(String message) {
        super(message);
    }

    public InactiveChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
