package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoucherController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherController Tests")
class VoucherControllerTest {

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private VoucherController voucherController;

    private UUID userId;
    private UUID voucherId;
    private SecurityUser securityUser;
    private VoucherResponse voucherResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        voucherId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.PREMIUM)
                .build();

        securityUser = new SecurityUser(user);

        voucherResponse = VoucherResponse.builder()
                .id(voucherId)
                .qrCode("VOUCHER123")
                .status(VoucherStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("GET /api/vouchers/my - Should return user's vouchers")
    void getMyVouchers_ReturnsUserVouchers() {
        // Given
        List<VoucherResponse> mockVouchers = new ArrayList<>();
        mockVouchers.add(voucherResponse);
        when(voucherService.getUserVouchers(userId)).thenReturn(mockVouchers);

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getMyVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(voucherId, response.getBody().get(0).getId());
        verify(voucherService, times(1)).getUserVouchers(userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/my - Should return empty list when no vouchers")
    void getMyVouchers_NoVouchers_ReturnsEmptyList() {
        // Given
        when(voucherService.getUserVouchers(userId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getMyVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(voucherService, times(1)).getUserVouchers(userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/my - Should return multiple vouchers")
    void getMyVouchers_MultipleVouchers_ReturnsAll() {
        // Given
        List<VoucherResponse> mockVouchers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockVouchers.add(VoucherResponse.builder()
                    .id(UUID.randomUUID())
                    .qrCode("VOUCHER" + i)
                    .status(VoucherStatus.ACTIVE)
                    .build());
        }
        when(voucherService.getUserVouchers(userId)).thenReturn(mockVouchers);

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getMyVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().size());
        verify(voucherService, times(1)).getUserVouchers(userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/my/active - Should return active vouchers only")
    void getActiveVouchers_ReturnsActiveVouchersOnly() {
        // Given
        List<VoucherResponse> mockVouchers = new ArrayList<>();
        mockVouchers.add(voucherResponse);
        when(voucherService.getActiveVouchers(userId)).thenReturn(mockVouchers);

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getActiveVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(VoucherStatus.ACTIVE, response.getBody().get(0).getStatus());
        verify(voucherService, times(1)).getActiveVouchers(userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/my/active - Should return empty list when no active vouchers")
    void getActiveVouchers_NoActiveVouchers_ReturnsEmptyList() {
        // Given
        when(voucherService.getActiveVouchers(userId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getActiveVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(voucherService, times(1)).getActiveVouchers(userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/{id} - Should return voucher by ID")
    void getVoucherById_ValidId_ReturnsVoucher() {
        // Given
        when(voucherService.getVoucherById(voucherId, userId)).thenReturn(voucherResponse);

        // When
        ResponseEntity<VoucherResponse> response =
                voucherController.getVoucherById(voucherId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(voucherId, response.getBody().getId());
        assertEquals("VOUCHER123", response.getBody().getQrCode());
        verify(voucherService, times(1)).getVoucherById(voucherId, userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/{id} - Should get different vouchers")
    void getVoucherById_DifferentIds_ReturnsDifferentVouchers() {
        // Given
        UUID voucherId2 = UUID.randomUUID();
        VoucherResponse voucher2 = VoucherResponse.builder()
                .id(voucherId2)
                .qrCode("VOUCHER456")
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherService.getVoucherById(voucherId, userId)).thenReturn(voucherResponse);
        when(voucherService.getVoucherById(voucherId2, userId)).thenReturn(voucher2);

        // When
        ResponseEntity<VoucherResponse> response1 =
                voucherController.getVoucherById(voucherId, securityUser);
        ResponseEntity<VoucherResponse> response2 =
                voucherController.getVoucherById(voucherId2, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(voucherId, response1.getBody().getId());
        assertEquals(voucherId2, response2.getBody().getId());
        assertEquals("VOUCHER123", response1.getBody().getQrCode());
        assertEquals("VOUCHER456", response2.getBody().getQrCode());
    }

    @Test
    @DisplayName("POST /api/vouchers/redeem/{qrCode} - Should redeem voucher successfully")
    void redeemVoucher_ValidQrCode_RedeemsVoucher() {
        // Given
        String qrCode = "VOUCHER123";
        VoucherResponse redeemedResponse = VoucherResponse.builder()
                .id(voucherId)
                .qrCode(qrCode)
                .status(VoucherStatus.REDEEMED)
                .redeemedAt(LocalDateTime.now())
                .build();

        when(voucherService.redeemVoucher(qrCode, userId)).thenReturn(redeemedResponse);

        // When
        ResponseEntity<VoucherResponse> response =
                voucherController.redeemVoucher(qrCode, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(VoucherStatus.REDEEMED, response.getBody().getStatus());
        assertEquals(qrCode, response.getBody().getQrCode());
        assertNotNull(response.getBody().getRedeemedAt());
        verify(voucherService, times(1)).redeemVoucher(qrCode, userId);
    }

    @Test
    @DisplayName("POST /api/vouchers/redeem/{qrCode} - Should redeem different vouchers")
    void redeemVoucher_DifferentQrCodes_RedeemsEach() {
        // Given
        String qrCode1 = "VOUCHER123";
        String qrCode2 = "VOUCHER456";

        VoucherResponse redeemed1 = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .qrCode(qrCode1)
                .status(VoucherStatus.REDEEMED)
                .build();

        VoucherResponse redeemed2 = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .qrCode(qrCode2)
                .status(VoucherStatus.REDEEMED)
                .build();

        when(voucherService.redeemVoucher(qrCode1, userId)).thenReturn(redeemed1);
        when(voucherService.redeemVoucher(qrCode2, userId)).thenReturn(redeemed2);

        // When
        ResponseEntity<VoucherResponse> response1 =
                voucherController.redeemVoucher(qrCode1, securityUser);
        ResponseEntity<VoucherResponse> response2 =
                voucherController.redeemVoucher(qrCode2, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(qrCode1, response1.getBody().getQrCode());
        assertEquals(qrCode2, response2.getBody().getQrCode());
        verify(voucherService, times(1)).redeemVoucher(qrCode1, userId);
        verify(voucherService, times(1)).redeemVoucher(qrCode2, userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/my/active - Should filter out redeemed vouchers")
    void getActiveVouchers_FiltersRedeemedVouchers() {
        // Given
        List<VoucherResponse> activeVouchers = new ArrayList<>();
        activeVouchers.add(VoucherResponse.builder()
                .id(UUID.randomUUID())
                .qrCode("ACTIVE1")
                .status(VoucherStatus.ACTIVE)
                .build());
        activeVouchers.add(VoucherResponse.builder()
                .id(UUID.randomUUID())
                .qrCode("ACTIVE2")
                .status(VoucherStatus.ACTIVE)
                .build());

        when(voucherService.getActiveVouchers(userId)).thenReturn(activeVouchers);

        // When
        ResponseEntity<List<VoucherResponse>> response =
                voucherController.getActiveVouchers(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().stream()
                .allMatch(v -> v.getStatus() == VoucherStatus.ACTIVE));
        verify(voucherService, times(1)).getActiveVouchers(userId);
    }
}
