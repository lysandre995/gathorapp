package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for voucher management.
 *
 * Endpoints:
 * - GET /api/vouchers/my → Get current user's vouchers
 * - GET /api/vouchers/my/active → Get active vouchers only
 * - GET /api/vouchers/{id} → Get specific voucher details
 * - POST /api/vouchers/redeem/{qrCode} → Redeem voucher (BUSINESS only)
 */
@Tag(name = "Vouchers", description = "Voucher and reward redemption APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * GET /api/vouchers/my
     * Get all vouchers for the current user.
     */
    @Operation(summary = "Get my vouchers", description = "Get all vouchers (active, redeemed, expired) for the current user")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PREMIUM', 'BUSINESS')")
    public ResponseEntity<List<VoucherResponse>> getMyVouchers(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/vouchers/my - User: {}", userId);

        List<VoucherResponse> vouchers = voucherService.getUserVouchers(userId);
        return ResponseEntity.ok(vouchers);
    }

    /**
     * GET /api/vouchers/my/active
     * Get only active (unredeemed, non-expired) vouchers for the current user.
     */
    @Operation(summary = "Get active vouchers", description = "Get only active vouchers that can still be redeemed")
    @GetMapping("/my/active")
    @PreAuthorize("hasAnyRole('PREMIUM', 'BUSINESS')")
    public ResponseEntity<List<VoucherResponse>> getActiveVouchers(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/vouchers/my/active - User: {}", userId);

        List<VoucherResponse> vouchers = voucherService.getActiveVouchers(userId);
        return ResponseEntity.ok(vouchers);
    }

    /**
     * GET /api/vouchers/{id}
     * Get details of a specific voucher.
     */
    @Operation(summary = "Get voucher details", description = "Get details of a specific voucher by ID")
    @GetMapping("/{id}")
    public ResponseEntity<VoucherResponse> getVoucherById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/vouchers/{} - User: {}", id, userId);

        VoucherResponse voucher = voucherService.getVoucherById(id, userId);
        return ResponseEntity.ok(voucher);
    }

    /**
     * POST /api/vouchers/redeem/{qrCode}
     * Redeem a voucher by scanning its QR code.
     * Only BUSINESS users can redeem vouchers.
     */
    @Operation(summary = "Redeem voucher", description = "Redeem a voucher by QR code. Only the business that created the reward can redeem.")
    @PostMapping("/redeem/{qrCode}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<VoucherResponse> redeemVoucher(
            @PathVariable("qrCode") String qrCode,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID businessUserId = securityUser.getUserId();
        log.info("POST /api/vouchers/redeem/{} - Business user: {}", qrCode, businessUserId);

        VoucherResponse voucher = voucherService.redeemVoucher(qrCode, businessUserId);
        return ResponseEntity.ok(voucher);
    }
}
