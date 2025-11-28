package com.alfano.gathorapp.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Review response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private ReviewerInfo reviewer;
    private UUID eventId;
    private UUID outingId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for reviewer information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerInfo {
        private UUID id;
        private String name;
    }
}