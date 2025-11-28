package com.alfano.gathorapp.outing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Outing response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutingResponse {
    private UUID id;
    private String title;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalDateTime outingDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private List<ParticipantInfo> participants;
    private Boolean isParticipant; // True if authenticated user is a participant
    private Boolean isFull; // True if outing reached max participants
    private OrganizerInfo organizer;
    private EventInfo event;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for participant information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private UUID id;
        private String name;
        private String email;
    }

    /**
     * Nested DTO for organizer information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizerInfo {
        private UUID id;
        private String name;
        private String email;
        private String role;
    }

    /**
     * Nested DTO for linked event information.
     * Null if outing is independent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventInfo {
        private UUID id;
        private String title;
        private LocalDateTime eventDate;
    }
}
