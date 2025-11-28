package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import com.alfano.gathorapp.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Outing entity and DTOs.
 */
@Component
public class OutingMapper {

    /**
     * Convert Outing entity to OutingResponse DTO.
     * @param outing The outing entity
     * @param currentUserId Optional ID of the authenticated user (for isParticipant check)
     */
    public OutingResponse toResponse(Outing outing, UUID currentUserId) {
        // Map participants
        List<OutingResponse.ParticipantInfo> participantList = outing.getParticipants().stream()
                .map(user -> OutingResponse.ParticipantInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());

        // Check if current user is a participant
        Boolean isParticipant = null;
        if (currentUserId != null) {
            isParticipant = outing.getParticipants().stream()
                    .anyMatch(user -> user.getId().equals(currentUserId));
        }

        OutingResponse.OutingResponseBuilder builder = OutingResponse.builder()
                .id(outing.getId())
                .title(outing.getTitle())
                .description(outing.getDescription())
                .location(outing.getLocation())
                .latitude(outing.getLatitude())
                .longitude(outing.getLongitude())
                .outingDate(outing.getOutingDate())
                .maxParticipants(outing.getMaxParticipants())
                .currentParticipants(outing.getCurrentParticipantCount())
                .participants(participantList)
                .isParticipant(isParticipant)
                .isFull(outing.isFull())
                .organizer(OutingResponse.OrganizerInfo.builder()
                        .id(outing.getOrganizer().getId())
                        .name(outing.getOrganizer().getName())
                        .email(outing.getOrganizer().getEmail())
                        .role(outing.getOrganizer().getRole().name())
                        .build())
                .createdAt(outing.getCreatedAt());

        // Add event info if outing is linked to an event
        if (outing.getEvent() != null) {
            builder.event(OutingResponse.EventInfo.builder()
                    .id(outing.getEvent().getId())
                    .title(outing.getEvent().getTitle())
                    .eventDate(outing.getEvent().getEventDate())
                    .build());
        }

        return builder.build();
    }

    /**
     * Convert Outing entity to OutingResponse DTO without user context.
     */
    public OutingResponse toResponse(Outing outing) {
        return toResponse(outing, null);
    }

    /**
     * Convert CreateOutingRequest DTO to Outing entity.
     */
    public Outing toEntity(CreateOutingRequest request, User organizer, Event event) {
        return Outing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .outingDate(request.getOutingDate())
                .maxParticipants(request.getMaxParticipants())
                .organizer(organizer)
                .event(event) // Can be null for independent outings
                .build();
    }
}
