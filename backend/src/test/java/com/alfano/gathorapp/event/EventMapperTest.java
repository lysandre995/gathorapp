package com.alfano.gathorapp.event;

import com.alfano.gathorapp.event.dto.CreateEventRequest;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventMapper.
 */
@DisplayName("EventMapper Tests")
class EventMapperTest {

    private EventMapper eventMapper;
    private User creator;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventMapper = new EventMapper();
        eventId = UUID.randomUUID();

        creator = User.builder()
                .id(UUID.randomUUID())
                .name("Event Creator")
                .email("creator@example.com")
                .role(Role.BUSINESS)
                .build();
    }

    @Test
    @DisplayName("Should convert Event to EventResponse")
    void toResponse_ConvertsCorrectly() {
        // Given
        LocalDateTime eventDate = LocalDateTime.now().plusDays(10);
        LocalDateTime createdAt = LocalDateTime.now();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .location("Test Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .eventDate(eventDate)
                .creator(creator)
                .createdAt(createdAt)
                .build();

        // When
        EventResponse response = eventMapper.toResponse(event);

        // Then
        assertNotNull(response);
        assertEquals(eventId, response.getId());
        assertEquals("Test Event", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals("Test Location", response.getLocation());
        assertEquals(45.4642, response.getLatitude());
        assertEquals(9.1900, response.getLongitude());
        assertEquals(eventDate, response.getEventDate());
        assertEquals(createdAt, response.getCreatedAt());

        assertNotNull(response.getCreator());
        assertEquals(creator.getId(), response.getCreator().getId());
        assertEquals("Event Creator", response.getCreator().getName());
        assertEquals("creator@example.com", response.getCreator().getEmail());
    }

    @Test
    @DisplayName("Should convert CreateEventRequest to Event entity")
    void toEntity_ConvertsCorrectly() {
        // Given
        LocalDateTime eventDate = LocalDateTime.now().plusDays(7);

        CreateEventRequest request = CreateEventRequest.builder()
                .title("New Event")
                .description("New Description")
                .location("New Location")
                .latitude(45.5)
                .longitude(9.2)
                .eventDate(eventDate)
                .build();

        // When
        Event event = eventMapper.toEntity(request, creator);

        // Then
        assertNotNull(event);
        assertEquals("New Event", event.getTitle());
        assertEquals("New Description", event.getDescription());
        assertEquals("New Location", event.getLocation());
        assertEquals(45.5, event.getLatitude());
        assertEquals(9.2, event.getLongitude());
        assertEquals(eventDate, event.getEventDate());
        assertEquals(creator, event.getCreator());
    }

    @Test
    @DisplayName("Should handle null description")
    void toResponse_NullDescription_HandlesCorrectly() {
        // Given
        Event event = Event.builder()
                .id(eventId)
                .title("Event Title")
                .description(null)
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .eventDate(LocalDateTime.now().plusDays(10))
                .creator(creator)
                .build();

        // When
        EventResponse response = eventMapper.toResponse(event);

        // Then
        assertNotNull(response);
        assertNull(response.getDescription());
    }

    @Test
    @DisplayName("Should map creator details correctly")
    void toResponse_CreatorDetails_MappedCorrectly() {
        // Given
        Event event = Event.builder()
                .id(eventId)
                .title("Event Title")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .eventDate(LocalDateTime.now().plusDays(10))
                .creator(creator)
                .build();

        // When
        EventResponse response = eventMapper.toResponse(event);

        // Then
        assertNotNull(response.getCreator());
        assertEquals(creator.getId(), response.getCreator().getId());
        assertEquals(creator.getName(), response.getCreator().getName());
        assertEquals(creator.getEmail(), response.getCreator().getEmail());
    }
}
