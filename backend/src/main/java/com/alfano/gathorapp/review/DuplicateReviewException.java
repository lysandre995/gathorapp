package com.alfano.gathorapp.review;

/**
 * Exception thrown when a user attempts to create a review for something they
 * already reviewed.
 * This exception will be mapped to HTTP 400 status by the
 * GlobalExceptionHandler.
 */
public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(String message) {
        super(message);
    }

    public DuplicateReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
