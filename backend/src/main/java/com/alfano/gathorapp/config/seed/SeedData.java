package com.alfano.gathorapp.config.seed;

import lombok.Data;

import java.util.List;

/**
 * Root DTO for seed data configuration.
 * Represents the entire seed-data.yml file structure.
 */
@Data
public class SeedData {
    private List<UserSeed> users;
    private List<EventSeed> events;
    private List<RewardSeed> rewards;
    private List<OutingSeed> outings;
}
