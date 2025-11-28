package com.alfano.gathorapp.config.seed;

import lombok.Data;

/**
 * DTO for event seed data.
 */
@Data
public class EventSeed {
    private String title;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private Integer daysFromNow;
    private Integer hour;
    private Integer minute;
    private String creatorEmail;
}
