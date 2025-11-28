package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OutingMapper.
 * Tests DTO <-> Entity conversion logic.
 */
@DisplayName("OutingMapper Tests")
class OutingMapperTest {

    private OutingMapper outingMapper;
    private User organizer;
    private Event event;
    private UUID outingId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        outingMapper = new OutingMapper();

        outingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Create organizer
        organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Organizer Name")
                .email("organizer@example.com")
                .role(Role.PREMIUM)
                .build();

        // Create event
        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .eventDate(LocalDateTime.now().plusDays(10))
                .build();
    }

    // ==================== toResponse Tests ====================

    @Test
    @DisplayName("Should convert Outing to OutingResponse without user context")
    void toResponse_WithoutUserContext_ConvertsCorrectly() {
        // Given
        Outing outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Description")
                .location("Test Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertEquals(outingId, response.getId());
        assertEquals("Test Outing", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals("Test Location", response.getLocation());
        assertEquals(45.4642, response.getLatitude());
        assertEquals(9.1900, response.getLongitude());
        assertEquals(10, response.getMaxParticipants());
        assertEquals(0, response.getCurrentParticipants());
        assertFalse(response.getIsFull());
        assertNull(response.getIsParticipant()); // No user context
        assertNotNull(response.getOrganizer());
        assertEquals(organizer.getId(), response.getOrganizer().getId());
    }

    @Test
    @DisplayName("Should include isParticipant=true when user is participant")
    void toResponse_UserIsParticipant_IsParticipantTrue() {
        // Given
        User participant = User.builder()
                .id(userId)
                .name("Participant")
                .email("participant@example.com")
                .role(Role.USER)
                .build();

        Set<User> participants = new HashSet<>();
        participants.add(participant);

        Outing outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .participants(participants)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing, userId);

        // Then
        assertNotNull(response);
        assertTrue(response.getIsParticipant());
        assertEquals(1, response.getCurrentParticipants());
        assertEquals(1, response.getParticipants().size());
        assertEquals(userId, response.getParticipants().get(0).getId());
    }

    @Test
    @DisplayName("Should include isParticipant=false when user is not participant")
    void toResponse_UserIsNotParticipant_IsParticipantFalse() {
        // Given
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Other User")
                .email("other@example.com")
                .role(Role.USER)
                .build();

        Set<User> participants = new HashSet<>();
        participants.add(otherUser);

        Outing outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .participants(participants)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing, userId);

        // Then
        assertNotNull(response);
        assertFalse(response.getIsParticipant());
        assertEquals(1, response.getCurrentParticipants());
    }

    @Test
    @DisplayName("Should include event info when outing is linked to event")
    void toResponse_WithEvent_IncludesEventInfo() {
        // Given
        Outing outing = Outing.builder()
                .id(outingId)
                .title("Event-linked Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .event(event)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertNotNull(response.getEvent());
        assertEquals(event.getId(), response.getEvent().getId());
        assertEquals("Test Event", response.getEvent().getTitle());
        assertEquals(event.getEventDate(), response.getEvent().getEventDate());
    }

    @Test
    @DisplayName("Should not include event info when outing is independent")
    void toResponse_WithoutEvent_NoEventInfo() {
        // Given
        Outing outing = Outing.builder()
                .id(outingId)
                .title("Independent Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .event(null) // No event
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertNull(response.getEvent());
    }

    @Test
    @DisplayName("Should mark outing as full when at max capacity")
    void toResponse_FullOuting_IsFullTrue() {
        // Given
        Set<User> participants = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            participants.add(User.builder()
                    .id(UUID.randomUUID())
                    .name("User " + i)
                    .email("user" + i + "@example.com")
                    .role(Role.USER)
                    .build());
        }

        Outing outing = Outing.builder()
                .id(outingId)
                .title("Full Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(5) // Same as participants count
                .organizer(organizer)
                .participants(participants)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertTrue(response.getIsFull());
        assertEquals(5, response.getCurrentParticipants());
        assertEquals(5, response.getMaxParticipants());
    }

    @Test
    @DisplayName("Should include all participant details in response")
    void toResponse_MultipleParticipants_IncludesAllDetails() {
        // Given
        User participant1 = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();

        User participant2 = User.builder()
                .id(UUID.randomUUID())
                .name("Bob")
                .email("bob@example.com")
                .role(Role.PREMIUM)
                .build();

        Set<User> participants = new HashSet<>();
        participants.add(participant1);
        participants.add(participant2);

        Outing outing = Outing.builder()
                .id(outingId)
                .title("Group Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .participants(participants)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getParticipants().size());
        assertTrue(response.getParticipants().stream()
                .anyMatch(p -> p.getName().equals("Alice")));
        assertTrue(response.getParticipants().stream()
                .anyMatch(p -> p.getName().equals("Bob")));
    }

    @Test
    @DisplayName("Should include organizer details in response")
    void toResponse_WithOrganizer_IncludesOrganizerDetails() {
        // Given
        Outing outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Description")
                .location("Location")
                .latitude(45.4642)
                .longitude(9.1900)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .organizer(organizer)
                .build();

        // When
        OutingResponse response = outingMapper.toResponse(outing);

        // Then
        assertNotNull(response);
        assertNotNull(response.getOrganizer());
        assertEquals(organizer.getId(), response.getOrganizer().getId());
        assertEquals("Organizer Name", response.getOrganizer().getName());
        assertEquals("organizer@example.com", response.getOrganizer().getEmail());
        assertEquals("PREMIUM", response.getOrganizer().getRole());
    }

    // ==================== toEntity Tests ====================

    @Test
    @DisplayName("Should convert CreateOutingRequest to Outing entity with event")
    void toEntity_WithEvent_ConvertsCorrectly() {
        // Given
        LocalDateTime outingDate = LocalDateTime.now().plusDays(7);
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("New Outing")
                .description("New Description")
                .location("New Location")
                .latitude(45.5)
                .longitude(9.2)
                .outingDate(outingDate)
                .maxParticipants(15)
                .eventId(event.getId())
                .build();

        // When
        Outing outing = outingMapper.toEntity(request, organizer, event);

        // Then
        assertNotNull(outing);
        assertEquals("New Outing", outing.getTitle());
        assertEquals("New Description", outing.getDescription());
        assertEquals("New Location", outing.getLocation());
        assertEquals(45.5, outing.getLatitude());
        assertEquals(9.2, outing.getLongitude());
        assertEquals(outingDate, outing.getOutingDate());
        assertEquals(15, outing.getMaxParticipants());
        assertEquals(organizer, outing.getOrganizer());
        assertEquals(event, outing.getEvent());
    }

    @Test
    @DisplayName("Should convert CreateOutingRequest to independent Outing without event")
    void toEntity_WithoutEvent_ConvertsCorrectly() {
        // Given
        LocalDateTime outingDate = LocalDateTime.now().plusDays(7);
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("Independent Outing")
                .description("Independent Description")
                .location("Independent Location")
                .latitude(45.5)
                .longitude(9.2)
                .outingDate(outingDate)
                .maxParticipants(8)
                .build();

        // When
        Outing outing = outingMapper.toEntity(request, organizer, null);

        // Then
        assertNotNull(outing);
        assertEquals("Independent Outing", outing.getTitle());
        assertEquals("Independent Description", outing.getDescription());
        assertEquals("Independent Location", outing.getLocation());
        assertEquals(45.5, outing.getLatitude());
        assertEquals(9.2, outing.getLongitude());
        assertEquals(outingDate, outing.getOutingDate());
        assertEquals(8, outing.getMaxParticipants());
        assertEquals(organizer, outing.getOrganizer());
        assertNull(outing.getEvent());
    }

    @Test
    @DisplayName("Should handle minimum maxParticipants value")
    void toEntity_MinParticipants_ConvertsCorrectly() {
        // Given
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("Small Outing")
                .description("Description")
                .location("Location")
                .latitude(45.5)
                .longitude(9.2)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(2) // Minimum
                .build();

        // When
        Outing outing = outingMapper.toEntity(request, organizer, null);

        // Then
        assertNotNull(outing);
        assertEquals(2, outing.getMaxParticipants());
    }

    @Test
    @DisplayName("Should handle large maxParticipants value")
    void toEntity_LargeParticipants_ConvertsCorrectly() {
        // Given
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("Large Outing")
                .description("Description")
                .location("Location")
                .latitude(45.5)
                .longitude(9.2)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(100) // Large number
                .build();

        // When
        Outing outing = outingMapper.toEntity(request, organizer, null);

        // Then
        assertNotNull(outing);
        assertEquals(100, outing.getMaxParticipants());
    }
}
