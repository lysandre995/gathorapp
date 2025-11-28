package com.alfano.gathorapp.outing.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for creating a new outing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 500, message = "Location must be between 3 and 500 characters")
    private String location;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Outing date is required")
    @Future(message = "Outing date must be in the future")
    private LocalDateTime outingDate;

    @NotNull(message = "Max participants is required")
    @Min(value = 2, message = "Max participants must be at least 2")
    @Max(value = 999, message = "Max participants cannot exceed 999")
    private Integer maxParticipants;

    /**
     * Optional: ID of the event this outing is linked to.
     * If null, the outing is independent.
     */
    private UUID eventId;
}
