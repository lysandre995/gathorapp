package com.alfano.gathorapp.report.dto;

import com.alfano.gathorapp.report.ReportStatus;
import com.alfano.gathorapp.report.ReportType;
import com.alfano.gathorapp.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for report responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private UUID id;
    private UserResponse reporter;
    private UserResponse reportedUser;
    private ReportType type;
    private String reason;
    private UUID relatedEntityId;
    private String relatedEntityType;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private UserResponse reviewedBy;
}
