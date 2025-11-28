package com.alfano.gathorapp.report;

import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Report entities to DTOs.
 */
@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final UserMapper userMapper;

    /**
     * Convert Report entity to ReportResponse DTO.
     */
    public ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
            .id(report.getId())
            .reporter(userMapper.toResponse(report.getReporter()))
            .reportedUser(userMapper.toResponse(report.getReportedUser()))
            .type(report.getType())
            .reason(report.getReason())
            .relatedEntityId(report.getRelatedEntityId())
            .relatedEntityType(report.getRelatedEntityType())
            .status(report.getStatus())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .reviewedBy(report.getReviewedBy() != null ? userMapper.toResponse(report.getReviewedBy()) : null)
            .build();
    }
}
