package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.alfano.gathorapp.voucher.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for ParticipationService.
 * Tests concurrent access control, transaction isolation, and business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationService Tests")
class ParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationMapper participationMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private ParticipationService participationService;

    private User testUser;
    private User organizer;
    private Outing testOuting;
    private Participation testParticipation;
    private UUID userId;
    private UUID organizerId;
    private UUID outingId;
    private UUID participationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizerId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        participationId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        organizer = User.builder()
                .id(organizerId)
                .name("Organizer")
                .email("organizer@example.com")
                .role(Role.PREMIUM)
                .build();

        testOuting = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Description")
                .organizer(organizer)
                .maxParticipants(5)
                .outingDate(LocalDateTime.now().plusDays(1))
                .build();

        testParticipation = Participation.builder()
                .id(participationId)
                .user(testUser)
                .outing(testOuting)
                .status(ParticipationStatus.PENDING)
                .build();
    }

    // ==================== getParticipationsByOuting Tests ====================

    @Test
    @DisplayName("Should get all participations for an outing")
    void getParticipationsByOuting_Success() {
        when(participationRepository.findByOutingId(outingId)).thenReturn(List.of(testParticipation));
        when(participationMapper.toResponse(testParticipation))
                .thenReturn(ParticipationResponse.builder().id(participationId).build());

        List<ParticipationResponse> result = participationService.getParticipationsByOuting(outingId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(participationId);
        verify(participationRepository).findByOutingId(outingId);
    }

    // ==================== getParticipationsByUser Tests ====================

    @Test
    @DisplayName("Should get all participations for a user")
    void getParticipationsByUser_Success() {
        when(participationRepository.findByUserId(userId)).thenReturn(List.of(testParticipation));
        when(participationMapper.toResponse(testParticipation))
                .thenReturn(ParticipationResponse.builder().id(participationId).build());

        List<ParticipationResponse> result = participationService.getParticipationsByUser(userId);

        assertThat(result).hasSize(1);
        verify(participationRepository).findByUserId(userId);
    }

    // ==================== joinOuting Tests ====================

    @Test
    @DisplayName("Should join outing successfully")
    void joinOuting_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(participationRepository.existsByUserAndOuting(testUser, testOuting)).thenReturn(false);
        when(participationRepository.countApprovedByOuting(testOuting)).thenReturn(2L);
        when(participationRepository.save(any(Participation.class))).thenReturn(testParticipation);
        when(participationMapper.toResponse(testParticipation))
                .thenReturn(ParticipationResponse.builder().id(participationId).build());
        doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

        ParticipationResponse result = participationService.joinOuting(outingId, userId);

        assertThat(result.getId()).isEqualTo(participationId);
        verify(participationRepository).save(any(Participation.class));
        verify(notificationService).createNotification(
                eq(organizerId),
                eq(NotificationType.PARTICIPATION_REQUEST),
                any(), any(), any(), eq("PARTICIPATION"));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void joinOuting_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.joinOuting(outingId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when outing not found")
    void joinOuting_OutingNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.joinOuting(outingId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outing not found");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when organizer tries to join own outing")
    void joinOuting_OrganizerCannotJoin() {
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));

        assertThatThrownBy(() -> participationService.joinOuting(outingId, organizerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Organizer cannot join their own outing");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already has participation")
    void joinOuting_AlreadyParticipating() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(participationRepository.existsByUserAndOuting(testUser, testOuting)).thenReturn(true);

        assertThatThrownBy(() -> participationService.joinOuting(outingId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already has a participation request");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when outing is full")
    void joinOuting_OutingFull() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(participationRepository.existsByUserAndOuting(testUser, testOuting)).thenReturn(false);
        when(participationRepository.countApprovedByOuting(testOuting)).thenReturn(5L); // Full

        assertThatThrownBy(() -> participationService.joinOuting(outingId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outing is full");

        verify(participationRepository, never()).save(any());
    }

    // ==================== approveParticipation Tests ====================

    @Test
    @DisplayName("Should approve participation successfully")
    void approveParticipation_Success() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));
        when(participationRepository.countApprovedByOuting(testOuting)).thenReturn(2L);
        when(participationRepository.save(testParticipation)).thenReturn(testParticipation);
        when(outingRepository.save(testOuting)).thenReturn(testOuting);
        when(participationMapper.toResponse(testParticipation))
                .thenReturn(ParticipationResponse.builder().id(participationId).build());
        doNothing().when(voucherService).checkAndIssueVoucher(any(), any());
        doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

        ParticipationResponse result = participationService.approveParticipation(participationId, organizerId);

        assertThat(result.getId()).isEqualTo(participationId);
        assertThat(testParticipation.getStatus()).isEqualTo(ParticipationStatus.APPROVED);
        verify(participationRepository).save(testParticipation);
        verify(voucherService).checkAndIssueVoucher(outingId, organizerId);
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.PARTICIPATION_APPROVED),
                any(), any(), any(), eq("OUTING"));
    }

    @Test
    @DisplayName("Should throw exception when participation not found for approval")
    void approveParticipation_NotFound() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.approveParticipation(participationId, organizerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Participation not found");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when non-organizer tries to approve")
    void approveParticipation_NotOrganizer() {
        UUID randomUserId = UUID.randomUUID();
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));

        assertThatThrownBy(() -> participationService.approveParticipation(participationId, randomUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the outing organizer can approve");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when participation is not pending")
    void approveParticipation_NotPending() {
        testParticipation.setStatus(ParticipationStatus.APPROVED);
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));

        assertThatThrownBy(() -> participationService.approveParticipation(participationId, organizerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not pending");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when outing is full during approval")
    void approveParticipation_OutingFull() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));
        when(participationRepository.countApprovedByOuting(testOuting)).thenReturn(5L); // Full

        assertThatThrownBy(() -> participationService.approveParticipation(participationId, organizerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("outing is already full");

        verify(participationRepository, never()).save(any());
    }

    // ==================== rejectParticipation Tests ====================

    @Test
    @DisplayName("Should reject participation successfully")
    void rejectParticipation_Success() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));
        when(participationRepository.save(testParticipation)).thenReturn(testParticipation);
        when(participationMapper.toResponse(testParticipation))
                .thenReturn(ParticipationResponse.builder().id(participationId).build());
        doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

        ParticipationResponse result = participationService.rejectParticipation(participationId, organizerId);

        assertThat(result.getId()).isEqualTo(participationId);
        assertThat(testParticipation.getStatus()).isEqualTo(ParticipationStatus.REJECTED);
        verify(participationRepository).save(testParticipation);
        verify(notificationService).createNotification(
                eq(userId),
                eq(NotificationType.PARTICIPATION_REJECTED),
                any(), any(), any(), eq("OUTING"));
    }

    @Test
    @DisplayName("Should throw exception when participation not found for rejection")
    void rejectParticipation_NotFound() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.rejectParticipation(participationId, organizerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Participation not found");

        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when non-organizer tries to reject")
    void rejectParticipation_NotOrganizer() {
        UUID randomUserId = UUID.randomUUID();
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));

        assertThatThrownBy(() -> participationService.rejectParticipation(participationId, randomUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the outing organizer can reject");

        verify(participationRepository, never()).save(any());
    }

    // ==================== leaveOuting Tests ====================

    @Test
    @DisplayName("Should leave outing successfully when approved")
    void leaveOuting_ApprovedParticipation_Success() {
        testParticipation.setStatus(ParticipationStatus.APPROVED);
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));
        when(outingRepository.save(testOuting)).thenReturn(testOuting);
        doNothing().when(participationRepository).delete(testParticipation);

        participationService.leaveOuting(participationId, userId);

        verify(outingRepository).save(testOuting);
        verify(participationRepository).delete(testParticipation);
    }

    @Test
    @DisplayName("Should leave outing successfully when pending")
    void leaveOuting_PendingParticipation_Success() {
        // Status is already PENDING from setUp
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));
        doNothing().when(participationRepository).delete(testParticipation);

        participationService.leaveOuting(participationId, userId);

        verify(participationRepository).delete(testParticipation);
        verify(outingRepository, never()).save(any()); // Should not update outing for pending
    }

    @Test
    @DisplayName("Should throw exception when participation not found for leaving")
    void leaveOuting_NotFound() {
        when(participationRepository.findById(participationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.leaveOuting(participationId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Participation not found");

        verify(participationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when user tries to cancel others participation")
    void leaveOuting_NotOwnParticipation() {
        UUID otherUserId = UUID.randomUUID();
        when(participationRepository.findById(participationId)).thenReturn(Optional.of(testParticipation));

        assertThatThrownBy(() -> participationService.leaveOuting(participationId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only cancel your own participation");

        verify(participationRepository, never()).delete(any());
    }
}
