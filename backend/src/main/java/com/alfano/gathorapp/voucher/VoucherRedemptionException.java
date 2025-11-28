package com.alfano.gathorapp.voucher;

/**
 * Exception thrown when a voucher cannot be redeemed.
 * Maps to HTTP 400 Bad Request.
 */
public class VoucherRedemptionException extends RuntimeException {
    public VoucherRedemptionException(String message) {
        super(message);
    }

    public VoucherRedemptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
