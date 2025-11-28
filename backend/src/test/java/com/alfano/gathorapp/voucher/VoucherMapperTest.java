package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VoucherMapper.
 */
@DisplayName("VoucherMapper Tests")
class VoucherMapperTest {

    private VoucherMapper voucherMapper;

    private UUID voucherId;
    private UUID rewardId;
    private UUID outingId;
    private UUID businessId;
    private User businessUser;
    private Reward reward;
    private Outing outing;
    private Voucher voucher;

    @BeforeEach
    void setUp() {
        voucherMapper = new VoucherMapper();

        voucherId = UUID.randomUUID();
        rewardId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        businessId = UUID.randomUUID();

        businessUser = User.builder()
                .id(businessId)
                .name("Test Business")
                .email("business@example.com")
                .passwordHash("hashedPassword")
                .role(Role.BUSINESS)
                .build();

        reward = Reward.builder()
                .id(rewardId)
                .title("Free Coffee")
                .description("One free coffee of any size")
                .business(businessUser)
                .build();

        outing = Outing.builder()
                .id(outingId)
                .title("Morning Run")
                .description("Test outing")
                .outingDate(LocalDateTime.of(2024, 12, 15, 8, 0))
                .build();

        voucher = Voucher.builder()
                .id(voucherId)
                .qrCode("VOUCHER123ABC")
                .status(VoucherStatus.ACTIVE)
                .reward(reward)
                .outing(outing)
                .issuedAt(LocalDateTime.of(2024, 12, 1, 10, 0))
                .expiresAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Test
    @DisplayName("toResponse - Should map voucher to response DTO correctly")
    void toResponse_ValidVoucher_MapsAllFieldsCorrectly() {
        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(voucherId, response.getId());
        assertEquals("VOUCHER123ABC", response.getQrCode());
        assertEquals(VoucherStatus.ACTIVE, response.getStatus());
        assertEquals(LocalDateTime.of(2024, 12, 1, 10, 0), response.getIssuedAt());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), response.getExpiresAt());
        assertNull(response.getRedeemedAt());
        assertNotNull(response.getCanBeRedeemed()); // canBeRedeemed depends on current time

        // Verify reward info
        assertNotNull(response.getReward());
        assertEquals(rewardId, response.getReward().getId());
        assertEquals("Free Coffee", response.getReward().getTitle());
        assertEquals("One free coffee of any size", response.getReward().getDescription());

        // Verify business info
        assertNotNull(response.getReward().getBusiness());
        assertEquals(businessId, response.getReward().getBusiness().getId());
        assertEquals("Test Business", response.getReward().getBusiness().getName());
        assertEquals("business@example.com", response.getReward().getBusiness().getEmail());

        // Verify outing info
        assertNotNull(response.getOuting());
        assertEquals(outingId, response.getOuting().getId());
        assertEquals("Morning Run", response.getOuting().getTitle());
        assertEquals(LocalDateTime.of(2024, 12, 15, 8, 0), response.getOuting().getOutingDate());
    }

    @Test
    @DisplayName("toResponse - Should map redeemed voucher correctly")
    void toResponse_RedeemedVoucher_MapsRedeemedFieldsCorrectly() {
        // Given
        LocalDateTime redeemedTime = LocalDateTime.of(2024, 12, 20, 14, 30);
        voucher.setStatus(VoucherStatus.REDEEMED);
        voucher.setRedeemedAt(redeemedTime);

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(VoucherStatus.REDEEMED, response.getStatus());
        assertEquals(redeemedTime, response.getRedeemedAt());
        assertFalse(response.getCanBeRedeemed());
    }

    @Test
    @DisplayName("toResponse - Should map expired voucher correctly")
    void toResponse_ExpiredVoucher_MapsExpiredStatusCorrectly() {
        // Given
        voucher.setStatus(VoucherStatus.EXPIRED);
        voucher.setExpiresAt(LocalDateTime.now().minusDays(1));

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(VoucherStatus.EXPIRED, response.getStatus());
        assertFalse(response.getCanBeRedeemed());
    }

    @Test
    @DisplayName("toResponse - Should map different rewards correctly")
    void toResponse_DifferentRewards_MapsDifferentRewardsCorrectly() {
        // Given
        Reward reward2 = Reward.builder()
                .id(UUID.randomUUID())
                .title("10% Discount")
                .description("Get 10% off on all items")
                .business(businessUser)
                .build();

        voucher.setReward(reward2);

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(reward2.getId(), response.getReward().getId());
        assertEquals("10% Discount", response.getReward().getTitle());
        assertEquals("Get 10% off on all items", response.getReward().getDescription());
    }

    @Test
    @DisplayName("toResponse - Should map different businesses correctly")
    void toResponse_DifferentBusinesses_MapsDifferentBusinessesCorrectly() {
        // Given
        User business2 = User.builder()
                .id(UUID.randomUUID())
                .name("Another Business")
                .email("another@example.com")
                .passwordHash("pass")
                .role(Role.BUSINESS)
                .build();

        reward.setBusiness(business2);

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(business2.getId(), response.getReward().getBusiness().getId());
        assertEquals("Another Business", response.getReward().getBusiness().getName());
        assertEquals("another@example.com", response.getReward().getBusiness().getEmail());
    }

    @Test
    @DisplayName("toResponse - Should map different outings correctly")
    void toResponse_DifferentOutings_MapsDifferentOutingsCorrectly() {
        // Given
        Outing outing2 = Outing.builder()
                .id(UUID.randomUUID())
                .title("Evening Walk")
                .description("Relaxing walk")
                .outingDate(LocalDateTime.of(2025, 1, 10, 18, 0))
                .build();

        voucher.setOuting(outing2);

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(outing2.getId(), response.getOuting().getId());
        assertEquals("Evening Walk", response.getOuting().getTitle());
        assertEquals(LocalDateTime.of(2025, 1, 10, 18, 0), response.getOuting().getOutingDate());
    }

    @Test
    @DisplayName("toResponse - Should preserve all timestamps correctly")
    void toResponse_DifferentTimestamps_MapsTimestampsCorrectly() {
        // Given
        LocalDateTime issuedTime = LocalDateTime.of(2023, 6, 15, 9, 30);
        LocalDateTime expiresTime = LocalDateTime.of(2023, 7, 15, 9, 30);
        LocalDateTime redeemedTime = LocalDateTime.of(2023, 6, 20, 14, 0);

        voucher.setIssuedAt(issuedTime);
        voucher.setExpiresAt(expiresTime);
        voucher.setRedeemedAt(redeemedTime);

        // When
        VoucherResponse response = voucherMapper.toResponse(voucher);

        // Then
        assertNotNull(response);
        assertEquals(issuedTime, response.getIssuedAt());
        assertEquals(expiresTime, response.getExpiresAt());
        assertEquals(redeemedTime, response.getRedeemedAt());
    }

    @Test
    @DisplayName("toResponse - Should map multiple vouchers independently")
    void toResponse_MultipleVouchers_MapsEachIndependently() {
        // Given
        Voucher voucher2 = Voucher.builder()
                .id(UUID.randomUUID())
                .qrCode("VOUCHER456XYZ")
                .status(VoucherStatus.REDEEMED)
                .reward(Reward.builder()
                        .id(UUID.randomUUID())
                        .title("Free Meal")
                        .description("One free meal")
                        .business(businessUser)
                        .build())
                .outing(Outing.builder()
                        .id(UUID.randomUUID())
                        .title("Bike Ride")
                        .outingDate(LocalDateTime.of(2025, 2, 1, 10, 0))
                        .build())
                .issuedAt(LocalDateTime.of(2024, 11, 1, 12, 0))
                .expiresAt(LocalDateTime.of(2024, 12, 1, 12, 0))
                .redeemedAt(LocalDateTime.of(2024, 11, 15, 16, 0))
                .build();

        // When
        VoucherResponse response1 = voucherMapper.toResponse(voucher);
        VoucherResponse response2 = voucherMapper.toResponse(voucher2);

        // Then
        assertNotNull(response1);
        assertNotNull(response2);

        // Verify they are independent
        assertNotEquals(response1.getId(), response2.getId());
        assertNotEquals(response1.getQrCode(), response2.getQrCode());
        assertEquals(VoucherStatus.ACTIVE, response1.getStatus());
        assertEquals(VoucherStatus.REDEEMED, response2.getStatus());
        assertEquals("Free Coffee", response1.getReward().getTitle());
        assertEquals("Free Meal", response2.getReward().getTitle());
    }

    @Test
    @DisplayName("toResponse - Should map canBeRedeemed flag correctly for different statuses")
    void toResponse_CanBeRedeemedFlag_MapsCorrectlyForDifferentStatuses() {
        // Active voucher with future expiration - can be redeemed
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setExpiresAt(LocalDateTime.now().plusDays(30));
        VoucherResponse activeResponse = voucherMapper.toResponse(voucher);
        assertTrue(activeResponse.getCanBeRedeemed());

        // Redeemed voucher - cannot be redeemed
        voucher.setStatus(VoucherStatus.REDEEMED);
        VoucherResponse redeemedResponse = voucherMapper.toResponse(voucher);
        assertFalse(redeemedResponse.getCanBeRedeemed());

        // Expired voucher - cannot be redeemed
        voucher.setStatus(VoucherStatus.EXPIRED);
        VoucherResponse expiredResponse = voucherMapper.toResponse(voucher);
        assertFalse(expiredResponse.getCanBeRedeemed());
    }
}
