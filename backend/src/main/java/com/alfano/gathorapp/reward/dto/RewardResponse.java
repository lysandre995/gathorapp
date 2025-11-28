package com.alfano.gathorapp.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Reward response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer requiredParticipants;
    private String qrCode;
    private UUID eventId;
    private String eventTitle;
    private BusinessInfo business;
    private LocalDateTime createdAt;

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
    }
}
