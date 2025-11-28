package com.alfano.gathorapp.config.seed;

import lombok.Data;

/**
 * DTO for reward seed data.
 */
@Data
public class RewardSeed {
    private String title;
    private String description;
    private Integer requiredParticipants;
    private String eventTitle;
    private String businessEmail;
}
