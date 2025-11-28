package com.alfano.gathorapp.participation.dto;

import com.alfano.gathorapp.participation.ParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Participation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationResponse {
    private UUID id;
    private UserInfo user;
    private OutingInfo outing;
    private ParticipationStatus status;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for user information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
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
        private Integer maxParticipants;
    }
}
