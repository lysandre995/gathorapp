package com.alfano.gathorapp.review;

/**
 * Exception thrown when a user is not authorized to perform a review operation.
 * This exception will be mapped to HTTP 403 status by the
 * GlobalExceptionHandler.
 */
public class UnauthorizedReviewAccessException extends RuntimeException {

    public UnauthorizedReviewAccessException(String message) {
        super(message);
    }

    public UnauthorizedReviewAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
