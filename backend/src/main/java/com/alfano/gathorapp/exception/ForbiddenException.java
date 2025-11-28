package com.alfano.gathorapp.exception;

/**
 * Exception thrown when a user attempts an action they don't have permission for.
 * This exception will be mapped to HTTP 403 status by the GlobalExceptionHandler.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
