package com.alfano.gathorapp.exception;

/**
 * Exception thrown when a requested resource is not found.
 * This exception will be mapped to HTTP 404 status by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
