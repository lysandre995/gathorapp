package com.alfano.gathorapp.voucher;

/**
 * Status of a voucher in its lifecycle.
 */
public enum VoucherStatus {
    /**
     * Voucher is active and can be redeemed.
     */
    ACTIVE,

    /**
     * Voucher has been successfully redeemed.
     */
    REDEEMED,

    /**
     * Voucher has expired and cannot be redeemed.
     */
    EXPIRED,

    /**
     * Voucher has been cancelled by admin or business.
     */
    CANCELLED
}
