package com.alfano.gathorapp.voucher;

/**
 * Exception thrown when a user tries to redeem a voucher they don't own.
 * Maps to HTTP 403 Forbidden.
 */
public class UnauthorizedVoucherAccessException extends RuntimeException {
    public UnauthorizedVoucherAccessException(String message) {
        super(message);
    }

    public UnauthorizedVoucherAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
