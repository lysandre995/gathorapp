package com.alfano.gathorapp.config.seed;

import lombok.Data;

/**
 * DTO for user seed data.
 */
@Data
public class UserSeed {
    private String name;
    private String email;
    private String password;
    private String role;
}
