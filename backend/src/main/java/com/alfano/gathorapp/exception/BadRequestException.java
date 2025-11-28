package com.alfano.gathorapp.exception;

/**
 * Exception thrown when a request is invalid or cannot be processed.
 * This exception will be mapped to HTTP 400 status by the GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
