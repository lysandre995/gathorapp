package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.reward.dto.RewardResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Reward entity and DTOs.
 */
@Component
public class RewardMapper {

    /**
     * Convert Reward entity to RewardResponse DTO.
     */
    public RewardResponse toResponse(Reward reward) {
        return RewardResponse.builder()
                .id(reward.getId())
                .title(reward.getTitle())
                .description(reward.getDescription())
                .requiredParticipants(reward.getRequiredParticipants())
                .qrCode(reward.getQrCode())
                .eventId(reward.getEvent().getId())
                .eventTitle(reward.getEvent().getTitle())
                .business(RewardResponse.BusinessInfo.builder()
                        .id(reward.getBusiness().getId())
                        .name(reward.getBusiness().getName())
                        .build())
                .createdAt(reward.getCreatedAt())
                .build();
    }
}
