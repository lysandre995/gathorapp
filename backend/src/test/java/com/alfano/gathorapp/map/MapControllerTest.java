package com.alfano.gathorapp.map;

import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MapController.
 * Uses Mockito to test controller logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MapController Tests")
class MapControllerTest {

    @Mock
    private MapService mapService;

    @InjectMocks
    private MapController mapController;

    private Double testLatitude;
    private Double testLongitude;
    private Double testRadius;
    private Integer testLimit;

    @BeforeEach
    void setUp() {
        testLatitude = 45.4642;  // Milan coordinates
        testLongitude = 9.1900;
        testRadius = 10.0;
        testLimit = 50;
    }

    @Test
    @DisplayName("GET /api/map/events/nearby - Should return nearby events")
    void getNearbyEvents_ReturnsEventList() {
        // Given
        List<EventResponse> mockEvents = new ArrayList<>();
        EventResponse event = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Nearby Event")
                .latitude(45.4700)
                .longitude(9.1950)
                .build();
        mockEvents.add(event);

        when(mapService.findNearbyEvents(testLatitude, testLongitude, testRadius, testLimit))
                .thenReturn(mockEvents);

        // When
        ResponseEntity<List<EventResponse>> response =
                mapController.getNearbyEvents(testLatitude, testLongitude, testRadius, testLimit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Nearby Event", response.getBody().get(0).getTitle());
        verify(mapService, times(1)).findNearbyEvents(testLatitude, testLongitude, testRadius, testLimit);
    }

    @Test
    @DisplayName("GET /api/map/events/nearby - Should return empty list when no events nearby")
    void getNearbyEvents_NoEvents_ReturnsEmptyList() {
        // Given
        when(mapService.findNearbyEvents(testLatitude, testLongitude, testRadius, testLimit))
                .thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<EventResponse>> response =
                mapController.getNearbyEvents(testLatitude, testLongitude, testRadius, testLimit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /api/map/outings/nearby - Should return nearby outings")
    void getNearbyOutings_ReturnsOutingList() {
        // Given
        List<OutingResponse> mockOutings = new ArrayList<>();
        OutingResponse outing = OutingResponse.builder()
                .id(UUID.randomUUID())
                .title("Nearby Outing")
                .latitude(45.4700)
                .longitude(9.1950)
                .build();
        mockOutings.add(outing);

        when(mapService.findNearbyOutings(testLatitude, testLongitude, testRadius, testLimit))
                .thenReturn(mockOutings);

        // When
        ResponseEntity<List<OutingResponse>> response =
                mapController.getNearbyOutings(testLatitude, testLongitude, testRadius, testLimit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Nearby Outing", response.getBody().get(0).getTitle());
        verify(mapService, times(1)).findNearbyOutings(testLatitude, testLongitude, testRadius, testLimit);
    }

    @Test
    @DisplayName("GET /api/map/outings/nearby - Should return empty list when no outings nearby")
    void getNearbyOutings_NoOutings_ReturnsEmptyList() {
        // Given
        when(mapService.findNearbyOutings(testLatitude, testLongitude, testRadius, testLimit))
                .thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<OutingResponse>> response =
                mapController.getNearbyOutings(testLatitude, testLongitude, testRadius, testLimit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /api/map/geocode - Should return location suggestions")
    void geocode_ReturnsLocationSuggestions() {
        // Given
        String query = "Milan";
        Integer limit = 5;

        List<LocationSuggestion> mockSuggestions = new ArrayList<>();
        LocationSuggestion suggestion = LocationSuggestion.builder()
                .displayName("Milan, Italy")
                .latitude("45.4642")
                .longitude("9.1900")
                .build();
        mockSuggestions.add(suggestion);

        when(mapService.geocode(query, limit)).thenReturn(mockSuggestions);

        // When
        ResponseEntity<List<LocationSuggestion>> response = mapController.geocode(query, limit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Milan, Italy", response.getBody().get(0).getDisplayName());
        verify(mapService, times(1)).geocode(query, limit);
    }

    @Test
    @DisplayName("GET /api/map/geocode - Should return empty list when no results")
    void geocode_NoResults_ReturnsEmptyList() {
        // Given
        String query = "NonExistentPlace12345";
        Integer limit = 5;

        when(mapService.geocode(query, limit)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<LocationSuggestion>> response = mapController.geocode(query, limit);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /api/map/reverse-geocode - Should return location from coordinates")
    void reverseGeocode_ReturnsLocation() {
        // Given
        LocationSuggestion mockLocation = LocationSuggestion.builder()
                .displayName("Piazza del Duomo, Milan, Italy")
                .latitude(String.valueOf(testLatitude))
                .longitude(String.valueOf(testLongitude))
                .build();

        when(mapService.reverseGeocode(testLatitude, testLongitude)).thenReturn(mockLocation);

        // When
        ResponseEntity<LocationSuggestion> response =
                mapController.reverseGeocode(testLatitude, testLongitude);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Piazza del Duomo, Milan, Italy", response.getBody().getDisplayName());
        assertEquals(String.valueOf(testLatitude), response.getBody().getLatitude());
        assertEquals(String.valueOf(testLongitude), response.getBody().getLongitude());
        verify(mapService, times(1)).reverseGeocode(testLatitude, testLongitude);
    }

    @Test
    @DisplayName("GET /api/map/events/nearby - Should handle multiple events")
    void getNearbyEvents_MultipleEvents_ReturnsAllEvents() {
        // Given
        List<EventResponse> mockEvents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mockEvents.add(EventResponse.builder()
                    .id(UUID.randomUUID())
                    .title("Event " + i)
                    .latitude(45.4700 + i * 0.01)
                    .longitude(9.1950 + i * 0.01)
                    .build());
        }

        when(mapService.findNearbyEvents(testLatitude, testLongitude, testRadius, testLimit))
                .thenReturn(mockEvents);

        // When
        ResponseEntity<List<EventResponse>> response =
                mapController.getNearbyEvents(testLatitude, testLongitude, testRadius, testLimit);

        // Then
        assertEquals(3, response.getBody().size());
    }

    @Test
    @DisplayName("GET /api/map/outings/nearby - Should handle different radius values")
    void getNearbyOutings_DifferentRadius_CallsServiceWithCorrectRadius() {
        // Given
        Double smallRadius = 5.0;
        Double largeRadius = 50.0;

        when(mapService.findNearbyOutings(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        mapController.getNearbyOutings(testLatitude, testLongitude, smallRadius, testLimit);
        mapController.getNearbyOutings(testLatitude, testLongitude, largeRadius, testLimit);

        // Then
        verify(mapService, times(1)).findNearbyOutings(testLatitude, testLongitude, smallRadius, testLimit);
        verify(mapService, times(1)).findNearbyOutings(testLatitude, testLongitude, largeRadius, testLimit);
    }

    @Test
    @DisplayName("GET /api/map/geocode - Should handle different query lengths")
    void geocode_DifferentQueryLengths_CallsService() {
        // Given
        String shortQuery = "NY";
        String longQuery = "New York City, United States of America";

        when(mapService.geocode(any(), any())).thenReturn(new ArrayList<>());

        // When
        mapController.geocode(shortQuery, 5);
        mapController.geocode(longQuery, 5);

        // Then
        verify(mapService, times(1)).geocode(shortQuery, 5);
        verify(mapService, times(1)).geocode(longQuery, 5);
    }
}
