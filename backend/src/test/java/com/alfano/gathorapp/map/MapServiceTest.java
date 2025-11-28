package com.alfano.gathorapp.map;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventMapper;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingMapper;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MapService geolocation features.
 *
 * Tests cover:
 * - Proximity search for events
 * - Proximity search for outings
 * - Distance calculation using Haversine formula
 */
@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private OutingMapper outingMapper;

    @InjectMocks
    private MapService mapService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail("test@example.com");
    }

    @Test
    void testFindNearbyEvents_WithinRadius_ReturnsEvents() {
        // Given - Milan coordinates: 45.4642, 9.1900
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 10.0;
        Integer limit = 10;

        // Create events at different locations
        Event nearEvent = createEvent("Near Event", 45.4700, 9.1950); // ~1km away
        Event farEvent = createEvent("Far Event", 45.5500, 9.3000); // ~15km away

        List<Event> allEvents = List.of(nearEvent, farEvent);
        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(allEvents);

        EventResponse nearResponse = new EventResponse();
        nearResponse.setId(nearEvent.getId());
        nearResponse.setTitle("Near Event");

        when(eventMapper.toResponse(nearEvent)).thenReturn(nearResponse);

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only near event should be returned
        assertEquals("Near Event", result.get(0).getTitle());
        verify(eventRepository, times(1)).findByEventDateAfter(any(LocalDateTime.class));
    }

    @Test
    void testFindNearbyEvents_NoEventsWithinRadius_ReturnsEmpty() {
        // Given - User in Milan, no events nearby
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 1.0; // Only 1km radius
        Integer limit = 10;

        Event farEvent = createEvent("Far Event", 45.5500, 9.3000); // ~15km away

        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(farEvent));

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindNearbyEvents_LimitResults_ReturnsMaxLimit() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 2;

        // Create 3 nearby events
        Event event1 = createEvent("Event 1", 45.4700, 9.1950);
        Event event2 = createEvent("Event 2", 45.4750, 9.2000);
        Event event3 = createEvent("Event 3", 45.4800, 9.2050);

        List<Event> allEvents = List.of(event1, event2, event3);
        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(allEvents);

        when(eventMapper.toResponse(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            EventResponse r = new EventResponse();
            r.setId(e.getId());
            r.setTitle(e.getTitle());
            return r;
        });

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // Limited to 2 results
    }

    @Test
    void testFindNearbyOutings_WithinRadius_ReturnsOutings() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 10.0;
        Integer limit = 10;

        Outing nearOuting = createOuting("Near Outing", 45.4700, 9.1950);
        Outing farOuting = createOuting("Far Outing", 45.5500, 9.3000);

        List<Outing> allOutings = List.of(nearOuting, farOuting);
        when(outingRepository.findByOutingDateAfter(any(LocalDateTime.class))).thenReturn(allOutings);

        OutingResponse nearResponse = new OutingResponse();
        nearResponse.setId(nearOuting.getId());
        nearResponse.setTitle("Near Outing");

        when(outingMapper.toResponse(nearOuting)).thenReturn(nearResponse);

        // When
        List<OutingResponse> result = mapService.findNearbyOutings(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Near Outing", result.get(0).getTitle());
    }

    @Test
    void testFindNearbyEvents_SortedByDistance_ReturnsNearestFirst() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 10;

        // Create events at different distances
        Event closeEvent = createEvent("Close", 45.4650, 9.1910); // Very close
        Event mediumEvent = createEvent("Medium", 45.4750, 9.2000); // Medium distance
        Event farEvent = createEvent("Far", 45.4900, 9.2100); // Farther

        List<Event> allEvents = List.of(farEvent, closeEvent, mediumEvent); // Unsorted

        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(allEvents);

        when(eventMapper.toResponse(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            EventResponse r = new EventResponse();
            r.setId(e.getId());
            r.setTitle(e.getTitle());
            return r;
        });

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Close", result.get(0).getTitle()); // Closest first
        assertEquals("Medium", result.get(1).getTitle()); // Medium second
        assertEquals("Far", result.get(2).getTitle()); // Farthest last
    }

    @Test
    void testFindNearbyEvents_EmptyList_ReturnsEmpty() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 10;

        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(List.of());

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindNearbyOutings_EmptyList_ReturnsEmpty() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 10;

        when(outingRepository.findByOutingDateAfter(any(LocalDateTime.class))).thenReturn(List.of());

        // When
        List<OutingResponse> result = mapService.findNearbyOutings(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindNearbyOutings_NoOutingsWithinRadius_ReturnsEmpty() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 1.0;
        Integer limit = 10;

        Outing farOuting = createOuting("Far Outing", 45.5500, 9.3000);

        when(outingRepository.findByOutingDateAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(farOuting));

        // When
        List<OutingResponse> result = mapService.findNearbyOutings(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindNearbyOutings_LimitResults_ReturnsMaxLimit() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 2;

        Outing outing1 = createOuting("Outing 1", 45.4700, 9.1950);
        Outing outing2 = createOuting("Outing 2", 45.4750, 9.2000);
        Outing outing3 = createOuting("Outing 3", 45.4800, 9.2050);

        List<Outing> allOutings = List.of(outing1, outing2, outing3);
        when(outingRepository.findByOutingDateAfter(any(LocalDateTime.class))).thenReturn(allOutings);

        when(outingMapper.toResponse(any(Outing.class))).thenAnswer(invocation -> {
            Outing o = invocation.getArgument(0);
            OutingResponse r = new OutingResponse();
            r.setId(o.getId());
            r.setTitle(o.getTitle());
            return r;
        });

        // When
        List<OutingResponse> result = mapService.findNearbyOutings(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindNearbyOutings_SortedByDistance_ReturnsNearestFirst() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 50.0;
        Integer limit = 10;

        Outing closeOuting = createOuting("Close", 45.4650, 9.1910);
        Outing mediumOuting = createOuting("Medium", 45.4750, 9.2000);
        Outing farOuting = createOuting("Far", 45.4900, 9.2100);

        List<Outing> allOutings = List.of(farOuting, closeOuting, mediumOuting);

        when(outingRepository.findByOutingDateAfter(any(LocalDateTime.class))).thenReturn(allOutings);

        when(outingMapper.toResponse(any(Outing.class))).thenAnswer(invocation -> {
            Outing o = invocation.getArgument(0);
            OutingResponse r = new OutingResponse();
            r.setId(o.getId());
            r.setTitle(o.getTitle());
            return r;
        });

        // When
        List<OutingResponse> result = mapService.findNearbyOutings(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Close", result.get(0).getTitle());
        assertEquals("Medium", result.get(1).getTitle());
        assertEquals("Far", result.get(2).getTitle());
    }

    @Test
    void testFindNearbyEvents_VerySmallRadius_FiltersCorrectly() {
        // Given
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 0.5; // 500 meters
        Integer limit = 10;

        Event veryCloseEvent = createEvent("Very Close", 45.4645, 9.1905); // ~50m
        Event slightlyFarEvent = createEvent("Slightly Far", 45.4700, 9.1950); // ~1km

        List<Event> allEvents = List.of(veryCloseEvent, slightlyFarEvent);
        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(allEvents);

        when(eventMapper.toResponse(veryCloseEvent)).thenAnswer(invocation -> {
            EventResponse r = new EventResponse();
            r.setId(veryCloseEvent.getId());
            r.setTitle("Very Close");
            return r;
        });

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Very Close", result.get(0).getTitle());
    }

    @Test
    void testFindNearbyEvents_ExactBoundary_IncludesEvent() {
        // Given - Test event right at the boundary
        Double userLat = 45.4642;
        Double userLon = 9.1900;
        Double radiusKm = 5.0;
        Integer limit = 10;

        Event boundaryEvent = createEvent("Boundary Event", 45.5000, 9.2000);

        List<Event> allEvents = List.of(boundaryEvent);
        when(eventRepository.findByEventDateAfter(any(LocalDateTime.class))).thenReturn(allEvents);

        when(eventMapper.toResponse(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            EventResponse r = new EventResponse();
            r.setId(e.getId());
            r.setTitle(e.getTitle());
            return r;
        });

        // When
        List<EventResponse> result = mapService.findNearbyEvents(userLat, userLon, radiusKm, limit);

        // Then - Should include event within radius
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // Helper methods

    private Event createEvent(String title, Double latitude, Double longitude) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(title);
        event.setDescription("Test event");
        event.setLocation("Test location");
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setCreator(user);
        return event;
    }

    private Outing createOuting(String title, Double latitude, Double longitude) {
        Outing outing = new Outing();
        outing.setId(UUID.randomUUID());
        outing.setTitle(title);
        outing.setDescription("Test outing");
        outing.setLocation("Test location");
        outing.setLatitude(latitude);
        outing.setLongitude(longitude);
        outing.setOutingDate(LocalDateTime.now().plusDays(7));
        outing.setMaxParticipants(10);
        outing.setOrganizer(user);
        return outing;
    }
}
