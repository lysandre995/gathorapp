package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Voucher entity and DTOs.
 */
@Component
public class VoucherMapper {

    /**
     * Convert Voucher entity to VoucherResponse DTO.
     */
    public VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .qrCode(voucher.getQrCode())
                .status(voucher.getStatus())
                .reward(VoucherResponse.RewardInfo.builder()
                        .id(voucher.getReward().getId())
                        .title(voucher.getReward().getTitle())
                        .description(voucher.getReward().getDescription())
                        .business(VoucherResponse.BusinessInfo.builder()
                                .id(voucher.getReward().getBusiness().getId())
                                .name(voucher.getReward().getBusiness().getName())
                                .email(voucher.getReward().getBusiness().getEmail())
                                .build())
                        .build())
                .outing(VoucherResponse.OutingInfo.builder()
                        .id(voucher.getOuting().getId())
                        .title(voucher.getOuting().getTitle())
                        .outingDate(voucher.getOuting().getOutingDate())
                        .build())
                .issuedAt(voucher.getIssuedAt())
                .redeemedAt(voucher.getRedeemedAt())
                .expiresAt(voucher.getExpiresAt())
                .canBeRedeemed(voucher.canBeRedeemed())
                .build();
    }
}
