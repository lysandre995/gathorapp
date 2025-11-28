package com.alfano.gathorapp.event;

import com.alfano.gathorapp.event.dto.CreateEventRequest;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.user.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Event entity and DTOs.
 */
@Component
public class EventMapper {

    /**
     * Convert Event entity to EventResponse DTO.
     */
    public EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .eventDate(event.getEventDate())
                .creator(EventResponse.CreatorInfo.builder()
                        .id(event.getCreator().getId())
                        .name(event.getCreator().getName())
                        .email(event.getCreator().getEmail())
                        .build())
                .createdAt(event.getCreatedAt())
                .build();
    }

    /**
     * Convert CreateEventRequest DTO to Event entity.
     */
    public Event toEntity(CreateEventRequest request, User creator) {
        return Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .eventDate(request.getEventDate())
                .creator(creator)
                .build();
    }
}
