package com.alfano.gathorapp.voucher.dto;

import com.alfano.gathorapp.voucher.VoucherStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Voucher response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private UUID id;
    private String qrCode;
    private VoucherStatus status;
    private RewardInfo reward;
    private OutingInfo outing;
    private LocalDateTime issuedAt;
    private LocalDateTime redeemedAt;
    private LocalDateTime expiresAt;
    private Boolean canBeRedeemed;

    /**
     * Nested DTO for reward information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardInfo {
        private UUID id;
        private String title;
        private String description;
        private BusinessInfo business;
    }

    /**
     * Nested DTO for business information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessInfo {
        private UUID id;
        private String name;
        private String email;
    }

    /**
     * Nested DTO for outing information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutingInfo {
        private UUID id;
        private String title;
        private LocalDateTime outingDate;
    }
}
