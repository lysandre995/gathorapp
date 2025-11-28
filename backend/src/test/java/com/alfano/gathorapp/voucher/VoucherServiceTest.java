package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.participation.ParticipationStatus;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.reward.RewardRepository;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoucherService.
 *
 * Tests cover:
 * - Voucher retrieval for users
 * - Voucher redemption with business validation
 * - Voucher expiration logic
 * - Access control and authorization
 */
@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoucherMapper voucherMapper;

    @InjectMocks
    private VoucherService voucherService;

    private UUID userId;
    private UUID businessUserId;
    private UUID voucherId;
    private User user;
    private User businessUser;
    private Reward reward;
    private Outing outing;
    private Voucher voucher;
    private com.alfano.gathorapp.event.Event event;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        businessUserId = UUID.randomUUID();
        voucherId = UUID.randomUUID();

        // Create Premium user
        user = new User();
        user.setId(userId);
        user.setName("Premium User");
        user.setEmail("premium@example.com");
        user.setRole(Role.PREMIUM);

        // Create Business user
        businessUser = new User();
        businessUser.setId(businessUserId);
        businessUser.setName("Business User");
        businessUser.setEmail("business@example.com");
        businessUser.setRole(Role.BUSINESS);

        // Create event
        event = new com.alfano.gathorapp.event.Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Coffee Event");
        event.setCreator(businessUser);

        // Create reward
        reward = new Reward();
        reward.setId(UUID.randomUUID());
        reward.setTitle("Free Coffee");
        reward.setDescription("Get a free coffee");
        reward.setRequiredParticipants(5);
        reward.setBusiness(businessUser);
        reward.setEvent(event);

        // Create outing
        outing = new Outing();
        outing.setId(UUID.randomUUID());
        outing.setTitle("Coffee Meetup");
        outing.setOrganizer(user);
        outing.setEvent(event);

        // Create voucher
        voucher = new Voucher();
        voucher.setId(voucherId);
        voucher.setQrCode("VOUCHER-TEST123");
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setUser(user);
        voucher.setReward(reward);
        voucher.setOuting(outing);
        voucher.setIssuedAt(LocalDateTime.now());
        voucher.setExpiresAt(LocalDateTime.now().plusDays(60));
    }

    @Test
    void testGetUserVouchers_Success() {
        // Given
        when(voucherRepository.findByUserId(userId)).thenReturn(List.of(voucher));

        VoucherResponse response = new VoucherResponse();
        response.setId(voucherId);
        response.setQrCode("VOUCHER-TEST123");

        when(voucherMapper.toResponse(voucher)).thenReturn(response);

        // When
        List<VoucherResponse> result = voucherService.getUserVouchers(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(voucherId, result.get(0).getId());
        verify(voucherRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetActiveVouchers_FiltersExpired() {
        // Given
        when(voucherRepository.findActiveVouchersByUserId(eq(userId), any(LocalDateTime.class)))
                .thenReturn(List.of(voucher));

        VoucherResponse response = new VoucherResponse();
        response.setId(voucherId);
        response.setCanBeRedeemed(true);

        when(voucherMapper.toResponse(voucher)).thenReturn(response);

        // When
        List<VoucherResponse> result = voucherService.getActiveVouchers(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getCanBeRedeemed());
    }

    @Test
    void testGetVoucherById_Success() {
        // Given
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        VoucherResponse response = new VoucherResponse();
        response.setId(voucherId);

        when(voucherMapper.toResponse(voucher)).thenReturn(response);

        // When
        VoucherResponse result = voucherService.getVoucherById(voucherId, userId);

        // Then
        assertNotNull(result);
        assertEquals(voucherId, result.getId());
    }

    @Test
    void testGetVoucherById_UnauthorizedUser_ThrowsException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.getVoucherById(voucherId, otherUserId);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
    }

    @Test
    void testRedeemVoucher_Success() {
        // Given
        String qrCode = "VOUCHER-TEST123";

        when(voucherRepository.findByQrCode(qrCode)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);

        VoucherResponse response = new VoucherResponse();
        response.setStatus(VoucherStatus.REDEEMED);

        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(response);

        // When
        VoucherResponse result = voucherService.redeemVoucher(qrCode, businessUserId);

        // Then
        assertNotNull(result);
        assertEquals(VoucherStatus.REDEEMED, result.getStatus());
        verify(voucherRepository, times(1)).save(any(Voucher.class));
    }

    @Test
    void testRedeemVoucher_WrongBusiness_ThrowsException() {
        // Given
        String qrCode = "VOUCHER-TEST123";
        UUID wrongBusinessId = UUID.randomUUID();

        when(voucherRepository.findByQrCode(qrCode)).thenReturn(Optional.of(voucher));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.redeemVoucher(qrCode, wrongBusinessId);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testRedeemVoucher_NotFound_ThrowsException() {
        // Given
        String qrCode = "INVALID-QR";

        when(voucherRepository.findByQrCode(qrCode)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.redeemVoucher(qrCode, businessUserId);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testExpireOldVouchers_UpdatesExpiredVouchers() {
        // Given
        Voucher expiredVoucher = new Voucher();
        expiredVoucher.setId(UUID.randomUUID());
        expiredVoucher.setStatus(VoucherStatus.ACTIVE);
        expiredVoucher.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(voucherRepository.findExpiredVouchers(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredVoucher));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(expiredVoucher);

        // When
        voucherService.expireOldVouchers();

        // Then
        verify(voucherRepository, times(1)).findExpiredVouchers(any(LocalDateTime.class));
        verify(voucherRepository, times(1)).save(argThat(v ->
                v.getStatus() == VoucherStatus.EXPIRED
        ));
    }

    @Test
    void testCheckAndIssueVoucher_EligiblePremiumUser_CreatesVoucher() {
        // Given
        UUID outingId = outing.getId();

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rewardRepository.findByEventId(event.getId())).thenReturn(List.of(reward));
        when(participationRepository.countByOutingAndStatus(outing, ParticipationStatus.APPROVED))
                .thenReturn(5L); // Meets requirement
        when(voucherRepository.findByUserId(userId)).thenReturn(List.of());
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(voucherRepository, times(1)).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_InsufficientParticipants_NoVoucherCreated() {
        // Given
        UUID outingId = outing.getId();

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rewardRepository.findByEventId(event.getId())).thenReturn(List.of(reward));
        when(participationRepository.countByOutingAndStatus(outing, ParticipationStatus.APPROVED))
                .thenReturn(3L); // Below requirement of 5

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_NonPremiumUser_NoVoucherCreated() {
        // Given
        UUID outingId = outing.getId();
        user.setRole(Role.USER); // Not premium

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_NotOrganizer_NoVoucherCreated() {
        // Given
        UUID outingId = outing.getId();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setRole(Role.PREMIUM);

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        // When
        voucherService.checkAndIssueVoucher(outingId, otherUserId);

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_NoEvent_NoVoucherCreated() {
        // Given
        UUID outingId = outing.getId();
        outing.setEvent(null); // No event linked

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(rewardRepository, never()).findByEventId(any());
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_NoRewards_NoVoucherCreated() {
        // Given
        UUID outingId = outing.getId();

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rewardRepository.findByEventId(event.getId())).thenReturn(List.of());

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_VoucherAlreadyExists_NoNewVoucherCreated() {
        // Given
        UUID outingId = outing.getId();

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rewardRepository.findByEventId(event.getId())).thenReturn(List.of(reward));
        when(participationRepository.countByOutingAndStatus(outing, ParticipationStatus.APPROVED))
                .thenReturn(5L);
        when(voucherRepository.findByUserId(userId)).thenReturn(List.of(voucher)); // Existing voucher

        // When
        voucherService.checkAndIssueVoucher(outingId, userId);

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testGetVoucherById_VoucherNotFound_ThrowsException() {
        // Given
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.getVoucherById(voucherId, userId);
        });

        assertTrue(exception.getMessage().contains("Voucher not found"));
    }

    @Test
    void testExpireOldVouchers_NoExpiredVouchers_NoUpdate() {
        // Given
        when(voucherRepository.findExpiredVouchers(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        voucherService.expireOldVouchers();

        // Then
        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    void testCheckAndIssueVoucher_OutingNotFound_ThrowsException() {
        // Given
        UUID outingId = UUID.randomUUID();
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.checkAndIssueVoucher(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
    }

    @Test
    void testCheckAndIssueVoucher_UserNotFound_ThrowsException() {
        // Given
        UUID outingId = outing.getId();
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voucherService.checkAndIssueVoucher(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }
}
