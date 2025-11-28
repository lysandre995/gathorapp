package com.alfano.gathorapp.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Event response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String title;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalDateTime eventDate;
    private CreatorInfo creator;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for event creator information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private UUID id;
        private String name;
        private String email;
    }
}
