package com.alfano.gathorapp.reward.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a reward.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRewardRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "Required participants is required")
    @Min(value = 1, message = "Required participants must be at least 1")
    private Integer requiredParticipants;

    @NotNull(message = "Event ID is required")
    private UUID eventId;
}
