package com.alfano.gathorapp.report;

import com.alfano.gathorapp.report.dto.CreateReportRequest;
import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user reports.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Create a new report (authenticated users only).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID reporterId = securityUser.getUserId();
        log.info("POST /api/reports - Creating report from user: {}", reporterId);

        ReportResponse response = reportService.createReport(request, reporterId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all pending reports (admin only).
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getPendingReports() {
        log.info("GET /api/reports/pending - Fetching pending reports");
        List<ReportResponse> reports = reportService.getAllPendingReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get all reports (admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        log.info("GET /api/reports - Fetching all reports");
        List<ReportResponse> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get report by ID (admin only).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable("id") UUID id) {
        log.info("GET /api/reports/{} - Fetching report", id);
        ReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports about a specific user (admin only).
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getReportsByUser(@PathVariable("userId") UUID userId) {
        log.info("GET /api/reports/user/{} - Fetching reports for user", userId);
        List<ReportResponse> reports = reportService.getReportsByReportedUser(userId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Mark report as reviewed (admin only).
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> reviewReport(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID adminId = securityUser.getUserId();
        log.info("PUT /api/reports/{}/review - Reviewing report by admin: {}", id, adminId);

        ReportResponse response = reportService.markAsReviewed(id, adminId);

        return ResponseEntity.ok(response);
    }

    /**
     * Dismiss report (admin only).
     */
    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> dismissReport(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID adminId = securityUser.getUserId();
        log.info("PUT /api/reports/{}/dismiss - Dismissing report by admin: {}", id, adminId);

        ReportResponse response = reportService.dismissReport(id, adminId);

        return ResponseEntity.ok(response);
    }
}
