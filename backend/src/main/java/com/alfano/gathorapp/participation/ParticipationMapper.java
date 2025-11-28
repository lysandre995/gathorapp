package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Participation entity and DTOs.
 */
@Component
public class ParticipationMapper {

    /**
     * Convert Participation entity to ParticipationResponse DTO.
     */
    public ParticipationResponse toResponse(Participation participation) {
        return ParticipationResponse.builder()
                .id(participation.getId())
                .user(ParticipationResponse.UserInfo.builder()
                        .id(participation.getUser().getId())
                        .name(participation.getUser().getName())
                        .email(participation.getUser().getEmail())
                        .build())
                .outing(ParticipationResponse.OutingInfo.builder()
                        .id(participation.getOuting().getId())
                        .title(participation.getOuting().getTitle())
                        .outingDate(participation.getOuting().getOutingDate())
                        .maxParticipants(participation.getOuting().getMaxParticipants())
                        .build())
                .status(participation.getStatus())
                .createdAt(participation.getCreatedAt())
                .build();
    }
}
