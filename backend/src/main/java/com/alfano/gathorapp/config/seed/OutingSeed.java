package com.alfano.gathorapp.config.seed;

import lombok.Data;

/**
 * DTO for outing seed data.
 */
@Data
public class OutingSeed {
    private String title;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private Integer daysFromNow;
    private Integer hour;
    private Integer minute;
    private Integer maxParticipants;
    private String organizerEmail;
    private String eventTitle;
}
