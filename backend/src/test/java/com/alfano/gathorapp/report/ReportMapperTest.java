package com.alfano.gathorapp.report;

import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserMapper;
import com.alfano.gathorapp.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportMapperTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ReportMapper reportMapper;

    private Report report;
    private User reporter;
    private User reported;
    private User reviewer;

    private UserResponse reporterResponse;
    private UserResponse reportedResponse;
    private UserResponse reviewerResponse;

    @BeforeEach
    void setup() {
        reporter = new User();
        reporter.setId(UUID.randomUUID());
        reporter.setName("Reporter");

        reported = new User();
        reported.setId(UUID.randomUUID());
        reported.setName("ReportedUser");

        reviewer = new User();
        reviewer.setId(UUID.randomUUID());
        reviewer.setName("AdminReviewer");

        reporterResponse = UserResponse.builder().id(reporter.getId()).name("Reporter").build();
        reportedResponse = UserResponse.builder().id(reported.getId()).name("ReportedUser").build();
        reviewerResponse = UserResponse.builder().id(reviewer.getId()).name("AdminReviewer").build();

        report = Report.builder()
                .id(UUID.randomUUID())
                .reporter(reporter)
                .reportedUser(reported)
                .type(ReportType.INAPPROPRIATE_BEHAVIOR)
                .reason("Bad behavior")
                .relatedEntityId(UUID.randomUUID())
                .relatedEntityType("POST")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .reviewedAt(LocalDateTime.now())
                .reviewedBy(reviewer)
                .build();
    }

    @Test
    void toResponse_allFieldsMappedCorrectly() {
        when(userMapper.toResponse(reporter)).thenReturn(reporterResponse);
        when(userMapper.toResponse(reported)).thenReturn(reportedResponse);
        when(userMapper.toResponse(reviewer)).thenReturn(reviewerResponse);

        ReportResponse result = reportMapper.toResponse(report);

        assertEquals(report.getId(), result.getId());
        assertEquals(reporterResponse, result.getReporter());
        assertEquals(reportedResponse, result.getReportedUser());
        assertEquals(ReportType.INAPPROPRIATE_BEHAVIOR, result.getType());
        assertEquals("Bad behavior", result.getReason());
        assertEquals(report.getRelatedEntityId(), result.getRelatedEntityId());
        assertEquals("POST", result.getRelatedEntityType());
        assertEquals(report.getStatus(), result.getStatus());
        assertEquals(report.getCreatedAt(), result.getCreatedAt());
        assertEquals(report.getReviewedAt(), result.getReviewedAt());
        assertEquals(reviewerResponse, result.getReviewedBy());

        verify(userMapper).toResponse(reporter);
        verify(userMapper).toResponse(reported);
        verify(userMapper).toResponse(reviewer);
    }

    @Test
    void toResponse_noReviewerMapsNullReviewedBy() {
        report.setReviewedBy(null);

        when(userMapper.toResponse(reporter)).thenReturn(reporterResponse);
        when(userMapper.toResponse(reported)).thenReturn(reportedResponse);

        ReportResponse result = reportMapper.toResponse(report);

        assertNull(result.getReviewedBy());

        verify(userMapper, never()).toResponse(eq(reviewer));

        verify(userMapper).toResponse(reporter);
        verify(userMapper).toResponse(reported);
    }
}
