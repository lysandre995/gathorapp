package com.alfano.gathorapp.report;

import com.alfano.gathorapp.report.dto.CreateReportRequest;
import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private UUID reporterId;
    private UUID adminId;
    private UUID reportId;
    private UUID targetUserId;

    private SecurityUser securityUser;
    private SecurityUser adminUser;

    private CreateReportRequest createRequest;
    private ReportResponse reportResponse;

    @BeforeEach
    void setup() {

        reporterId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        User reporter = new User();
        reporter.setId(reporterId);
        reporter.setName("user@test.com");
        reporter.setPasswordHash("password");
        reporter.setRole(Role.USER);
        reporter.setBanned(false);
        reporter.setCreatedAt(LocalDateTime.now());

        User admin = new User();
        admin.setId(adminId);
        admin.setName("admin@test.com");
        admin.setPasswordHash("password");
        admin.setRole(Role.ADMIN);
        admin.setBanned(false);
        admin.setCreatedAt(LocalDateTime.now());

        securityUser = new SecurityUser(reporter);
        adminUser = new SecurityUser(admin);

        createRequest = new CreateReportRequest();
        createRequest.setReportedUserId(targetUserId);
        createRequest.setType(ReportType.FAKE_IDENTITY);
        createRequest.setReason("Not genuine");

        reportResponse = new ReportResponse();
        reportResponse.setId(reportId);
    }

    @Test
    void createReport_success() {
        when(reportService.createReport(createRequest, reporterId)).thenReturn(reportResponse);

        ResponseEntity<ReportResponse> result = reportController.createReport(createRequest, securityUser);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(reportResponse, result.getBody());

        verify(reportService).createReport(createRequest, reporterId);
    }

    @Test
    void getPendingReports_success() {
        when(reportService.getAllPendingReports()).thenReturn(List.of(reportResponse));

        ResponseEntity<List<ReportResponse>> result = reportController.getPendingReports();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
        verify(reportService).getAllPendingReports();
    }

    @Test
    void getAllReports_success() {
        when(reportService.getAllReports()).thenReturn(List.of(reportResponse));

        ResponseEntity<List<ReportResponse>> result = reportController.getAllReports();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
        verify(reportService).getAllReports();
    }

    @Test
    void getReportById_success() {
        when(reportService.getReportById(reportId)).thenReturn(reportResponse);

        ResponseEntity<ReportResponse> result = reportController.getReportById(reportId);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(reportResponse, result.getBody());
        verify(reportService).getReportById(reportId);
    }

    @Test
    void getReportsByUser_success() {
        when(reportService.getReportsByReportedUser(targetUserId))
                .thenReturn(List.of(reportResponse));

        ResponseEntity<List<ReportResponse>> result = reportController.getReportsByUser(targetUserId);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
        verify(reportService).getReportsByReportedUser(targetUserId);
    }

    @Test
    void reviewReport_success() {
        when(reportService.markAsReviewed(reportId, adminId)).thenReturn(reportResponse);

        ResponseEntity<ReportResponse> result = reportController.reviewReport(reportId, adminUser);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(reportResponse, result.getBody());

        verify(reportService).markAsReviewed(reportId, adminId);
    }

    @Test
    void dismissReport_success() {
        when(reportService.dismissReport(reportId, adminId)).thenReturn(reportResponse);

        ResponseEntity<ReportResponse> result = reportController.dismissReport(reportId, adminUser);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(reportResponse, result.getBody());

        verify(reportService).dismissReport(reportId, adminId);
    }
}
