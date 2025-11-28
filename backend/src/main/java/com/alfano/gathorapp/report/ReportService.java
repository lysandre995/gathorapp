package com.alfano.gathorapp.report;

import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.report.dto.CreateReportRequest;
import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final NotificationService notificationService;

    /**
     * Create a new report.
     * Sends notification to all admins.
     */
    @Transactional
    public ReportResponse createReport(CreateReportRequest request, UUID reporterId) {
        log.info("Creating report from user {} against user {}", reporterId, request.getReportedUserId());

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new RuntimeException("Reporter not found"));

        User reportedUser = userRepository.findById(request.getReportedUserId())
            .orElseThrow(() -> new RuntimeException("Reported user not found"));

        // Prevent self-reporting
        if (reporterId.equals(request.getReportedUserId())) {
            throw new RuntimeException("Cannot report yourself");
        }

        Report report = Report.builder()
            .reporter(reporter)
            .reportedUser(reportedUser)
            .type(request.getType())
            .reason(request.getReason())
            .relatedEntityId(request.getRelatedEntityId())
            .relatedEntityType(request.getRelatedEntityType())
            .status(ReportStatus.PENDING)
            .build();

        report = reportRepository.save(report);
        log.info("Report created with ID: {}", report.getId());

        // Notify all admins about the new report
        notifyAdmins(report);

        return reportMapper.toResponse(report);
    }

    /**
     * Get all pending reports (admin only).
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllPendingReports() {
        log.info("Fetching all pending reports");
        return reportRepository.findAllPending().stream()
            .map(reportMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all reports (admin only).
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        log.info("Fetching all reports");
        return reportRepository.findAll().stream()
            .map(reportMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get report by ID (admin only).
     */
    @Transactional(readOnly = true)
    public ReportResponse getReportById(UUID reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));
        return reportMapper.toResponse(report);
    }

    /**
     * Mark report as reviewed (admin only).
     */
    @Transactional
    public ReportResponse markAsReviewed(UUID reportId, UUID adminId) {
        log.info("Admin {} reviewing report {}", adminId, reportId);

        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        report.markAsReviewed(admin);
        report = reportRepository.save(report);

        return reportMapper.toResponse(report);
    }

    /**
     * Dismiss report (admin only).
     */
    @Transactional
    public ReportResponse dismissReport(UUID reportId, UUID adminId) {
        log.info("Admin {} dismissing report {}", adminId, reportId);

        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        report.dismiss(admin);
        report = reportRepository.save(report);

        return reportMapper.toResponse(report);
    }

    /**
     * Get all reports about a specific user (admin only).
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByReportedUser(UUID userId) {
        log.info("Fetching reports for user {}", userId);
        return reportRepository.findByReportedUserId(userId).stream()
            .map(reportMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Notify all admins about a new report.
     */
    private void notifyAdmins(Report report) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        String message = String.format(
            "New report from %s about %s for %s",
            report.getReporter().getName(),
            report.getReportedUser().getName(),
            report.getType()
        );

        admins.forEach(admin ->
            notificationService.createNotification(
                admin.getId(),
                NotificationType.NEW_REPORT,
                "New User Report",
                message,
                report.getId(),
                "REPORT"
            )
        );

        log.info("Notified {} admins about report {}", admins.size(), report.getId());
    }
}
