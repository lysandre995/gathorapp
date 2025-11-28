package com.alfano.gathorapp.report;

import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.report.dto.CreateReportRequest;
import com.alfano.gathorapp.report.dto.ReportResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportService reportService;

    private UUID reporterId;
    private UUID reportedId;
    private UUID adminId;

    private User reporter;
    private User reportedUser;
    private User admin;

    private Report report;
    private ReportResponse response;
    private CreateReportRequest request;

    @BeforeEach
    void setup() {
        reporterId = UUID.randomUUID();
        reportedId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        reporter = new User();
        reporter.setId(reporterId);
        reporter.setName("Reporter");

        reportedUser = new User();
        reportedUser.setId(reportedId);
        reportedUser.setName("ReportedUser");

        admin = new User();
        admin.setId(adminId);
        admin.setName("Admin");

        request = new CreateReportRequest();
        request.setReportedUserId(reportedId);
        request.setType(ReportType.FAKE_IDENTITY);
        request.setReason("Bad behavior");

        report = Report.builder()
                .id(UUID.randomUUID())
                .reporter(reporter)
                .reportedUser(reportedUser)
                .status(ReportStatus.PENDING)
                .type(request.getType())
                .reason(request.getReason())
                .build();

        response = new ReportResponse();
    }

    @Test
    void createReport_success() {
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedId)).thenReturn(Optional.of(reportedUser));
        when(reportRepository.save(any())).thenReturn(report);
        when(reportMapper.toResponse(report)).thenReturn(response);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        ReportResponse result = reportService.createReport(request, reporterId);

        assertNotNull(result);
        verify(reportRepository).save(any());
        verify(notificationService).createNotification(
                eq(admin.getId()),
                eq(NotificationType.NEW_REPORT),
                eq("New User Report"),
                anyString(),
                eq(report.getId()),
                eq("REPORT"));
    }

    @Test
    void createReport_selfReport_throws() {
        request.setReportedUserId(reporterId);

        assertThrows(RuntimeException.class, () -> reportService.createReport(request, reporterId));
    }

    @Test
    void getAllPendingReports_success() {
        when(reportRepository.findAllPending()).thenReturn(List.of(report));
        when(reportMapper.toResponse(report)).thenReturn(response);

        List<ReportResponse> list = reportService.getAllPendingReports();

        assertEquals(1, list.size());
        verify(reportRepository).findAllPending();
    }

    @Test
    void getAllReports_success() {
        when(reportRepository.findAll()).thenReturn(List.of(report));
        when(reportMapper.toResponse(report)).thenReturn(response);

        List<ReportResponse> list = reportService.getAllReports();

        assertEquals(1, list.size());
        verify(reportRepository).findAll();
    }

    @Test
    void getReportById_success() {
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportMapper.toResponse(report)).thenReturn(response);

        ReportResponse result = reportService.getReportById(report.getId());

        assertNotNull(result);
        verify(reportRepository).findById(report.getId());
    }

    @Test
    void markAsReviewed_success() {
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.toResponse(report)).thenReturn(response);

        ReportResponse result = reportService.markAsReviewed(report.getId(), adminId);

        assertNotNull(result);
        verify(reportRepository).save(report);
    }

    @Test
    void dismissReport_success() {
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.toResponse(report)).thenReturn(response);

        ReportResponse result = reportService.dismissReport(report.getId(), adminId);

        assertNotNull(result);
        verify(reportRepository).save(report);
    }

    @Test
    void getReportsByReportedUser_success() {
        when(reportRepository.findByReportedUserId(reportedId)).thenReturn(List.of(report));
        when(reportMapper.toResponse(report)).thenReturn(response);

        List<ReportResponse> list = reportService.getReportsByReportedUser(reportedId);

        assertEquals(1, list.size());
        verify(reportRepository).findByReportedUserId(reportedId);
    }
}
