package com.alfano.gathorapp.report.dto;

import com.alfano.gathorapp.report.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a new report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    /**
     * ID of the user being reported.
     */
    @NotNull(message = "Reported user ID is required")
    private UUID reportedUserId;

    /**
     * Type of report.
     */
    @NotNull(message = "Report type is required")
    private ReportType type;

    /**
     * Additional reason (optional free text).
     */
    private String reason;

    /**
     * ID of related entity (Event or Outing).
     */
    private UUID relatedEntityId;

    /**
     * Type of related entity ("EVENT" or "OUTING").
     */
    private String relatedEntityType;
}
