package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutingService.
 *
 * Tests cover:
 * - Outing creation and retrieval
 * - Join/Leave functionality with validation
 * - Participant management
 * - Access control and authorization
 * - Max participants limit enforcement
 */
@ExtendWith(MockitoExtension.class)
class OutingServiceTest {

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutingMapper outingMapper;

    @Mock
    private com.alfano.gathorapp.pattern.strategy.UserStrategyFactory strategyFactory;

    @InjectMocks
    private OutingService outingService;

    private UUID userId;
    private UUID organizerId;
    private UUID eventId;
    private UUID outingId;
    private User user;
    private User organizer;
    private Event event;
    private Outing outing;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        outingId = UUID.randomUUID();

        // Create regular user
        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        // Create organizer
        organizer = User.builder()
                .id(organizerId)
                .name("Organizer")
                .email("organizer@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        // Create event
        event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .location("Test Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .eventDate(LocalDateTime.now().plusDays(7))
                .creator(organizer)
                .build();

        // Create outing
        outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Outing Description")
                .location("Test Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(5)
                .organizer(organizer)
                .event(event)
                .participants(new HashSet<>())
                .build();
    }

    @Test
    void testCreateOuting_Success() {
        // Given
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("New Outing")
                .description("New Description")
                .location("New Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .eventId(eventId)
                .build();

        // Mock strategy
        com.alfano.gathorapp.pattern.strategy.UserLimitationStrategy mockStrategy = mock(
                com.alfano.gathorapp.pattern.strategy.UserLimitationStrategy.class);

        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(strategyFactory.getStrategy(organizer)).thenReturn(mockStrategy);
        doNothing().when(mockStrategy).validateParticipantCount(10);
        when(mockStrategy.canCreateEventLinkedOuting(organizer)).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Mock the mapper to return an outing entity from the request
        when(outingMapper.toEntity(any(CreateOutingRequest.class), eq(organizer), eq(event))).thenReturn(outing);
        when(outingRepository.save(any(Outing.class))).thenReturn(outing);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("New Outing")
                .build();

        when(outingMapper.toResponse(any(Outing.class))).thenReturn(response);

        // When
        OutingResponse result = outingService.createOuting(request, organizerId);

        // Then
        assertNotNull(result);
        assertEquals(outingId, result.getId());
        verify(outingRepository, times(1)).save(any(Outing.class));
    }

    @Test
    void testCreateOuting_EventNotFound_ThrowsException() {
        // Given
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("New Outing")
                .eventId(eventId)
                .maxParticipants(5)
                .build();

        // Mock strategy
        com.alfano.gathorapp.pattern.strategy.UserLimitationStrategy mockStrategy = mock(
                com.alfano.gathorapp.pattern.strategy.UserLimitationStrategy.class);

        doNothing().when(mockStrategy).validateParticipantCount(5);

        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(strategyFactory.getStrategy(organizer)).thenReturn(mockStrategy);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.createOuting(request, organizerId);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testGetOutingById_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .currentParticipants(0)
                .maxParticipants(5)
                .isParticipant(false)
                .isFull(false)
                .build();

        when(outingMapper.toResponse(outing, userId)).thenReturn(response);

        // When
        OutingResponse result = outingService.getOutingById(outingId, userId);

        // Then
        assertNotNull(result);
        assertEquals(outingId, result.getId());
        assertFalse(result.getIsParticipant());
        verify(outingRepository, times(1)).findById(outingId);
    }

    @Test
    void testGetOutingById_NotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.getOutingById(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
    }

    @Test
    void testJoinOuting_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(outingRepository.save(any(Outing.class))).thenReturn(outing);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .currentParticipants(1)
                .isParticipant(true)
                .build();

        when(outingMapper.toResponse(any(Outing.class), eq(userId))).thenReturn(response);

        // When
        OutingResponse result = outingService.joinOuting(outingId, userId);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsParticipant());
        assertEquals(1, result.getCurrentParticipants());
        verify(outingRepository, times(1)).save(any(Outing.class));
    }

    @Test
    void testJoinOuting_OutingNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.joinOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testJoinOuting_UserNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.joinOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testJoinOuting_OrganizerCannotJoin_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.joinOuting(outingId, organizerId);
        });

        assertTrue(exception.getMessage().contains("Organizer cannot join"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testJoinOuting_OutingFull_ThrowsException() {
        // Given
        // Fill the outing to max capacity
        for (int i = 0; i < 5; i++) {
            User participant = User.builder()
                    .id(UUID.randomUUID())
                    .name("Participant " + i)
                    .email("participant" + i + "@example.com")
                    .role(Role.USER)
                    .build();
            outing.addParticipant(participant);
        }

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.joinOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing is full"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testJoinOuting_AlreadyParticipant_ThrowsException() {
        // Given
        outing.addParticipant(user);

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.joinOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("already a participant"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testLeaveOuting_Success() {
        // Given
        outing.addParticipant(user);

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(outingRepository.save(any(Outing.class))).thenReturn(outing);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .currentParticipants(0)
                .isParticipant(false)
                .build();

        when(outingMapper.toResponse(any(Outing.class), eq(userId))).thenReturn(response);

        // When
        OutingResponse result = outingService.leaveOuting(outingId, userId);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsParticipant());
        assertEquals(0, result.getCurrentParticipants());
        verify(outingRepository, times(1)).save(any(Outing.class));
    }

    @Test
    void testLeaveOuting_NotParticipant_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.leaveOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("not a participant"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testGetAllOutings_Success() {
        // Given
        List<Outing> outings = Arrays.asList(outing);
        when(outingRepository.findAll()).thenReturn(outings);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        when(outingMapper.toResponse(outing)).thenReturn(response);

        // When
        List<OutingResponse> result = outingService.getAllOutings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(outingId, result.get(0).getId());
        verify(outingRepository, times(1)).findAll();
    }

    @Test
    void testDeleteOuting_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        doNothing().when(outingRepository).delete(outing);

        // When
        outingService.deleteOuting(outingId, organizerId);

        // Then
        verify(outingRepository, times(1)).delete(outing);
    }

    @Test
    void testDeleteOuting_Unauthorized_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.deleteOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("organizer") || exception.getMessage().contains("delete"));
        verify(outingRepository, never()).delete(any(Outing.class));
    }

    @Test
    void testDeleteOuting_OutingNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.deleteOuting(outingId, organizerId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(outingRepository, never()).delete(any(Outing.class));
    }

    @Test
    void testGetUpcomingOutings_Success() {
        // Given
        List<Outing> upcomingOutings = Arrays.asList(outing);
        when(outingRepository.findUpcomingOutings(any(LocalDateTime.class))).thenReturn(upcomingOutings);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .outingDate(LocalDateTime.now().plusDays(7))
                .build();

        when(outingMapper.toResponse(outing)).thenReturn(response);

        // When
        List<OutingResponse> result = outingService.getUpcomingOutings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(outingId, result.get(0).getId());
        verify(outingRepository, times(1)).findUpcomingOutings(any(LocalDateTime.class));
    }

    @Test
    void testGetUpcomingOutings_EmptyList() {
        // Given
        when(outingRepository.findUpcomingOutings(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When
        List<OutingResponse> result = outingService.getUpcomingOutings();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(outingRepository, times(1)).findUpcomingOutings(any(LocalDateTime.class));
    }

    @Test
    void testGetOutingsByOrganizer_Success() {
        // Given
        List<Outing> outings = Arrays.asList(outing);
        when(outingRepository.findByOrganizerId(organizerId)).thenReturn(outings);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        when(outingMapper.toResponse(outing)).thenReturn(response);

        // When
        List<OutingResponse> result = outingService.getOutingsByOrganizer(organizerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(outingId, result.get(0).getId());
        verify(outingRepository, times(1)).findByOrganizerId(organizerId);
    }

    @Test
    void testGetOutingsByOrganizer_NoOutings() {
        // Given
        when(outingRepository.findByOrganizerId(organizerId)).thenReturn(Collections.emptyList());

        // When
        List<OutingResponse> result = outingService.getOutingsByOrganizer(organizerId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(outingRepository, times(1)).findByOrganizerId(organizerId);
    }

    @Test
    void testGetOutingsByEvent_Success() {
        // Given
        List<Outing> outings = Arrays.asList(outing);
        when(outingRepository.findByEventId(eventId)).thenReturn(outings);

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        when(outingMapper.toResponse(outing)).thenReturn(response);

        // When
        List<OutingResponse> result = outingService.getOutingsByEvent(eventId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(outingId, result.get(0).getId());
        verify(outingRepository, times(1)).findByEventId(eventId);
    }

    @Test
    void testGetOutingsByEvent_NoOutings() {
        // Given
        when(outingRepository.findByEventId(eventId)).thenReturn(Collections.emptyList());

        // When
        List<OutingResponse> result = outingService.getOutingsByEvent(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(outingRepository, times(1)).findByEventId(eventId);
    }

    @Test
    void testGetAllOutings_EmptyList() {
        // Given
        when(outingRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<OutingResponse> result = outingService.getAllOutings();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(outingRepository, times(1)).findAll();
    }

    @Test
    void testGetOutingById_WithoutUserContext_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));

        OutingResponse response = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        when(outingMapper.toResponse(outing, null)).thenReturn(response);

        // When
        OutingResponse result = outingService.getOutingById(outingId);

        // Then
        assertNotNull(result);
        assertEquals(outingId, result.getId());
        verify(outingRepository, times(1)).findById(outingId);
        verify(outingMapper, times(1)).toResponse(outing, null);
    }

    @Test
    void testLeaveOuting_OutingNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.leaveOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    void testLeaveOuting_UserNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outingService.leaveOuting(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(outingRepository, never()).save(any(Outing.class));
    }
}
