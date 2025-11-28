package com.alfano.gathorapp.event;

import com.alfano.gathorapp.event.dto.CreateEventRequest;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.event.dto.UpdateEventRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventService.
 *
 * Tests cover:
 * - Event creation (BUSINESS user only)
 * - Event retrieval and listing
 * - Event update (creator only)
 * - Event deletion (creator only)
 * - Access control and authorization
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    private UUID businessUserId;
    private UUID regularUserId;
    private UUID eventId;
    private User businessUser;
    private User regularUser;
    private Event event;

    @BeforeEach
    void setUp() {
        businessUserId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        // Create BUSINESS user
        businessUser = User.builder()
                .id(businessUserId)
                .name("Business User")
                .email("business@example.com")
                .passwordHash("hashedPassword")
                .role(Role.BUSINESS)
                .build();

        // Create regular USER
        regularUser = User.builder()
                .id(regularUserId)
                .name("Regular User")
                .email("user@example.com")
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
                .creator(businessUser)
                .build();
    }

    @Test
    void testGetAllEvents_Success() {
        // Given
        List<Event> events = Arrays.asList(event);
        when(eventRepository.findAll()).thenReturn(events);

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        when(eventMapper.toResponse(event)).thenReturn(response);

        // When
        List<EventResponse> result = eventService.getAllEvents();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(eventId, result.get(0).getId());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void testGetUpcomingEvents_Success() {
        // Given
        List<Event> events = Arrays.asList(event);
        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class))).thenReturn(events);

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        when(eventMapper.toResponse(event)).thenReturn(response);

        // When
        List<EventResponse> result = eventService.getUpcomingEvents();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findUpcomingEvents(any(LocalDateTime.class));
    }

    @Test
    void testGetEventById_Success() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        when(eventMapper.toResponse(event)).thenReturn(response);

        // When
        EventResponse result = eventService.getEventById(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void testGetEventById_NotFound_ThrowsException() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.getEventById(eventId);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void testGetEventsByCreator_Success() {
        // Given
        List<Event> events = Arrays.asList(event);
        when(eventRepository.findByCreatorId(businessUserId)).thenReturn(events);

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        when(eventMapper.toResponse(event)).thenReturn(response);

        // When
        List<EventResponse> result = eventService.getEventsByCreator(businessUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByCreatorId(businessUserId);
    }

    @Test
    void testCreateEvent_BusinessUser_Success() {
        // Given
        CreateEventRequest request = CreateEventRequest.builder()
                .title("New Event")
                .description("New Description")
                .location("New Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .eventDate(LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.findById(businessUserId)).thenReturn(Optional.of(businessUser));
        when(eventMapper.toEntity(request, businessUser)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("New Event")
                .build();

        when(eventMapper.toResponse(event)).thenReturn(response);

        // When
        EventResponse result = eventService.createEvent(request, businessUserId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void testCreateEvent_NonBusinessUser_ThrowsException() {
        // Given
        CreateEventRequest request = CreateEventRequest.builder()
                .title("New Event")
                .build();

        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.createEvent(request, regularUserId);
        });

        assertTrue(exception.getMessage().contains("Only BUSINESS users can create events"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testCreateEvent_UserNotFound_ThrowsException() {
        // Given
        CreateEventRequest request = CreateEventRequest.builder()
                .title("New Event")
                .build();

        when(userRepository.findById(businessUserId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.createEvent(request, businessUserId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_Creator_Success() {
        // Given
        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Updated Title")
                .build();

        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        // When
        EventResponse result = eventService.updateEvent(eventId, request, businessUserId);

        // Then
        assertNotNull(result);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_NotCreator_ThrowsException() {
        // Given
        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Title")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.updateEvent(eventId, request, regularUserId);
        });

        assertTrue(exception.getMessage().contains("Only the event creator can update"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_EventNotFound_ThrowsException() {
        // Given
        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Title")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.updateEvent(eventId, request, businessUserId);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testDeleteEvent_Creator_Success() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        doNothing().when(eventRepository).delete(event);

        // When
        eventService.deleteEvent(eventId, businessUserId);

        // Then
        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    void testDeleteEvent_NotCreator_ThrowsException() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.deleteEvent(eventId, regularUserId);
        });

        assertTrue(exception.getMessage().contains("Only the event creator can delete"));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void testDeleteEvent_EventNotFound_ThrowsException() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.deleteEvent(eventId, businessUserId);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository, never()).delete(any(Event.class));
    }
}
