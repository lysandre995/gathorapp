package com.alfano.gathorapp.map;

import com.alfano.gathorapp.event.EventMapper;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.OutingMapper;
import com.alfano.gathorapp.outing.OutingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MapService geocoding methods.
 * Tests the geocode and reverseGeocode methods that interact with external
 * APIs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MapService Geocode Tests")
@SuppressWarnings("unchecked")
class MapServiceGeocodeTest {

        @Mock
        private EventRepository eventRepository;

        @Mock
        private OutingRepository outingRepository;

        @Mock
        private EventMapper eventMapper;

        @Mock
        private OutingMapper outingMapper;

        @Mock
        private RestTemplate restTemplate;

        private MapService mapService;

        @BeforeEach
        void setUp() {
                mapService = new MapService(eventRepository, outingRepository, eventMapper, outingMapper);
                // Inject the mocked RestTemplate using reflection
                ReflectionTestUtils.setField(mapService, "restTemplate", restTemplate);
        }

        @Test
        @DisplayName("geocode - Should return location suggestions for valid query")
        void geocode_ValidQuery_ReturnsLocationSuggestions() {
                // Given
                String query = "Milan, Italy";
                Integer limit = 5;

                List<LocationSuggestion> mockSuggestions = new ArrayList<>();
                LocationSuggestion suggestion = LocationSuggestion.builder()
                                .displayName("Milan, Lombardy, Italy")
                                .latitude("45.4642")
                                .longitude("9.1900")
                                .type("city")
                                .importance(0.9)
                                .build();
                mockSuggestions.add(suggestion);

                ResponseEntity<List<LocationSuggestion>> mockResponse = ResponseEntity.ok(mockSuggestions);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

                // When
                List<LocationSuggestion> results = mapService.geocode(query, limit);

                // Then
                assertNotNull(results);
                assertEquals(1, results.size());
                assertEquals("Milan, Lombardy, Italy", results.get(0).getDisplayName());
                assertEquals("45.4642", results.get(0).getLatitude());
                assertEquals("9.1900", results.get(0).getLongitude());
                verify(restTemplate, times(1)).exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class));
        }

        @Test
        @DisplayName("geocode - Should return multiple suggestions")
        void geocode_MultipleResults_ReturnsAllSuggestions() {
                // Given
                String query = "Rome";
                Integer limit = 3;

                List<LocationSuggestion> mockSuggestions = new ArrayList<>();
                mockSuggestions.add(LocationSuggestion.builder()
                                .displayName("Rome, Italy")
                                .latitude("41.9028")
                                .longitude("12.4964")
                                .type("city")
                                .build());
                mockSuggestions.add(LocationSuggestion.builder()
                                .displayName("Rome, Georgia, USA")
                                .latitude("34.2570")
                                .longitude("-85.1647")
                                .type("city")
                                .build());
                mockSuggestions.add(LocationSuggestion.builder()
                                .displayName("Rome, New York, USA")
                                .latitude("43.2128")
                                .longitude("-75.4557")
                                .type("city")
                                .build());

                ResponseEntity<List<LocationSuggestion>> mockResponse = ResponseEntity.ok(mockSuggestions);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

                // When
                List<LocationSuggestion> results = mapService.geocode(query, limit);

                // Then
                assertNotNull(results);
                assertEquals(3, results.size());
                assertEquals("Rome, Italy", results.get(0).getDisplayName());
                assertEquals("Rome, Georgia, USA", results.get(1).getDisplayName());
                assertEquals("Rome, New York, USA", results.get(2).getDisplayName());
        }

        @Test
        @DisplayName("geocode - Should return empty list when no results found")
        void geocode_NoResults_ReturnsEmptyList() {
                // Given
                String query = "NonExistentPlace12345XYZ";
                Integer limit = 5;

                List<LocationSuggestion> emptyList = new ArrayList<>();
                ResponseEntity<List<LocationSuggestion>> mockResponse = ResponseEntity.ok(emptyList);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

                // When
                List<LocationSuggestion> results = mapService.geocode(query, limit);

                // Then
                assertNotNull(results);
                assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("geocode - Should return empty list when response body is null")
        void geocode_NullResponseBody_ReturnsEmptyList() {
                // Given
                String query = "Test Query";
                Integer limit = 5;

                ResponseEntity<List<LocationSuggestion>> mockResponse = ResponseEntity.ok(null);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

                // When
                List<LocationSuggestion> results = mapService.geocode(query, limit);

                // Then
                assertNotNull(results);
                assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("geocode - Should handle API exception and return empty list")
        void geocode_ApiException_ReturnsEmptyList() {
                // Given
                String query = "Test Query";
                Integer limit = 5;

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("API error"));

                // When
                List<LocationSuggestion> results = mapService.geocode(query, limit);

                // Then
                assertNotNull(results);
                assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("geocode - Should handle different query types")
        void geocode_DifferentQueryTypes_HandlesCorrectly() {
                // Test with address
                String addressQuery = "123 Main St, New York";
                List<LocationSuggestion> addressResults = new ArrayList<>();
                addressResults.add(LocationSuggestion.builder()
                                .displayName("123 Main St, New York, NY, USA")
                                .latitude("40.7128")
                                .longitude("-74.0060")
                                .build());

                ResponseEntity<List<LocationSuggestion>> mockResponse1 = ResponseEntity.ok(addressResults);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse1);

                List<LocationSuggestion> results1 = mapService.geocode(addressQuery, 5);
                assertEquals(1, results1.size());

                // Test with landmark
                String landmarkQuery = "Colosseum";
                List<LocationSuggestion> landmarkResults = new ArrayList<>();
                landmarkResults.add(LocationSuggestion.builder()
                                .displayName("Colosseum, Rome, Italy")
                                .latitude("41.8902")
                                .longitude("12.4922")
                                .build());

                ResponseEntity<List<LocationSuggestion>> mockResponse2 = ResponseEntity.ok(landmarkResults);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse2);

                List<LocationSuggestion> results2 = mapService.geocode(landmarkQuery, 5);
                assertEquals(1, results2.size());
        }

        @Test
        @DisplayName("reverseGeocode - Should return location for valid coordinates")
        void reverseGeocode_ValidCoordinates_ReturnsLocation() {
                // Given
                Double latitude = 45.4642;
                Double longitude = 9.1900;

                LocationSuggestion mockLocation = LocationSuggestion.builder()
                                .displayName("Piazza del Duomo, Milan, Lombardy, Italy")
                                .latitude("45.4642")
                                .longitude("9.1900")
                                .type("square")
                                .address(LocationSuggestion.AddressComponents.builder()
                                                .road("Piazza del Duomo")
                                                .city("Milan")
                                                .state("Lombardy")
                                                .country("Italy")
                                                .countryCode("IT")
                                                .build())
                                .build();

                ResponseEntity<LocationSuggestion> mockResponse = ResponseEntity.ok(mockLocation);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse);

                // When
                LocationSuggestion result = mapService.reverseGeocode(latitude, longitude);

                // Then
                assertNotNull(result);
                assertEquals("Piazza del Duomo, Milan, Lombardy, Italy", result.getDisplayName());
                assertEquals("45.4642", result.getLatitude());
                assertEquals("9.1900", result.getLongitude());
                verify(restTemplate, times(1)).exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class));
        }

        @Test
        @DisplayName("reverseGeocode - Should handle different coordinate locations")
        void reverseGeocode_DifferentLocations_ReturnsCorrectLocations() {
                // Test Milan
                Double milanLat = 45.4642;
                Double milanLon = 9.1900;

                LocationSuggestion milanLocation = LocationSuggestion.builder()
                                .displayName("Milan, Italy")
                                .latitude("45.4642")
                                .longitude("9.1900")
                                .build();

                ResponseEntity<LocationSuggestion> milanResponse = ResponseEntity.ok(milanLocation);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(milanResponse);

                LocationSuggestion result1 = mapService.reverseGeocode(milanLat, milanLon);
                assertEquals("Milan, Italy", result1.getDisplayName());

                // Test Rome
                Double romeLat = 41.9028;
                Double romeLon = 12.4964;

                LocationSuggestion romeLocation = LocationSuggestion.builder()
                                .displayName("Rome, Italy")
                                .latitude("41.9028")
                                .longitude("12.4964")
                                .build();

                ResponseEntity<LocationSuggestion> romeResponse = ResponseEntity.ok(romeLocation);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(romeResponse);

                LocationSuggestion result2 = mapService.reverseGeocode(romeLat, romeLon);
                assertEquals("Rome, Italy", result2.getDisplayName());
        }

        @Test
        @DisplayName("reverseGeocode - Should handle API exception and return fallback location")
        void reverseGeocode_ApiException_ReturnsFallbackLocation() {
                // Given
                Double latitude = 45.4642;
                Double longitude = 9.1900;

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenThrow(new RuntimeException("API error"));

                // When
                LocationSuggestion result = mapService.reverseGeocode(latitude, longitude);

                // Then
                assertNotNull(result);
                assertEquals("45.4642", result.getLatitude());
                assertEquals("9.19", result.getLongitude());
                assertTrue(result.getDisplayName().contains("45.464200"));
                assertTrue(result.getDisplayName().contains("9.190000"));
        }

        @Test
        @DisplayName("reverseGeocode - Should handle null response and return fallback location")
        void reverseGeocode_NullResponse_ReturnsFallbackLocation() {
                // Given
                Double latitude = 0.0;
                Double longitude = 0.0;

                ResponseEntity<LocationSuggestion> mockResponse = ResponseEntity.ok(null);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse);

                // When
                LocationSuggestion result = mapService.reverseGeocode(latitude, longitude);

                // Then - Service returns null when response body is null
                assertNull(result);
        }

        @Test
        @DisplayName("reverseGeocode - Should handle negative coordinates")
        void reverseGeocode_NegativeCoordinates_HandlesCorrectly() {
                // Given - Southern and Western hemisphere
                Double latitude = -33.8688;
                Double longitude = -118.4085;

                LocationSuggestion mockLocation = LocationSuggestion.builder()
                                .displayName("Los Angeles, California, USA")
                                .latitude("-33.8688")
                                .longitude("-118.4085")
                                .build();

                ResponseEntity<LocationSuggestion> mockResponse = ResponseEntity.ok(mockLocation);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse);

                // When
                LocationSuggestion result = mapService.reverseGeocode(latitude, longitude);

                // Then
                assertNotNull(result);
                assertEquals("-33.8688", result.getLatitude());
                assertEquals("-118.4085", result.getLongitude());
        }

        @Test
        @DisplayName("reverseGeocode - Should handle zero coordinates")
        void reverseGeocode_ZeroCoordinates_HandlesCorrectly() {
                // Given - Null Island (0, 0)
                Double latitude = 0.0;
                Double longitude = 0.0;

                LocationSuggestion mockLocation = LocationSuggestion.builder()
                                .displayName("Gulf of Guinea")
                                .latitude("0.0")
                                .longitude("0.0")
                                .build();

                ResponseEntity<LocationSuggestion> mockResponse = ResponseEntity.ok(mockLocation);

                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse);

                // When
                LocationSuggestion result = mapService.reverseGeocode(latitude, longitude);

                // Then
                assertNotNull(result);
                assertEquals("0.0", result.getLatitude());
                assertEquals("0.0", result.getLongitude());
        }

        @Test
        @DisplayName("geocode - Should handle different limit values")
        void geocode_DifferentLimits_RespectsLimit() {
                // Test limit 1
                String query = "Paris";
                List<LocationSuggestion> suggestions = new ArrayList<>();
                suggestions.add(LocationSuggestion.builder()
                                .displayName("Paris, France")
                                .latitude("48.8566")
                                .longitude("2.3522")
                                .build());

                ResponseEntity<List<LocationSuggestion>> mockResponse = ResponseEntity.ok(suggestions);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

                List<LocationSuggestion> results = mapService.geocode(query, 1);
                assertNotNull(results);
                assertEquals(1, results.size());
        }

        @Test
        @DisplayName("reverseGeocode - Should handle extreme coordinates")
        void reverseGeocode_ExtremeCoordinates_HandlesCorrectly() {
                // Test North Pole
                Double northPoleLat = 90.0;
                Double northPoleLon = 0.0;

                LocationSuggestion northPole = LocationSuggestion.builder()
                                .displayName("North Pole")
                                .latitude("90.0")
                                .longitude("0.0")
                                .build();

                ResponseEntity<LocationSuggestion> mockResponse1 = ResponseEntity.ok(northPole);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse1);

                LocationSuggestion result1 = mapService.reverseGeocode(northPoleLat, northPoleLon);
                assertNotNull(result1);
                assertEquals("90.0", result1.getLatitude());

                // Test South Pole
                Double southPoleLat = -90.0;
                Double southPoleLon = 0.0;

                LocationSuggestion southPole = LocationSuggestion.builder()
                                .displayName("South Pole")
                                .latitude("-90.0")
                                .longitude("0.0")
                                .build();

                ResponseEntity<LocationSuggestion> mockResponse2 = ResponseEntity.ok(southPole);
                when(restTemplate.exchange(
                                anyString(),
                                eq(HttpMethod.GET),
                                isNull(),
                                eq(LocationSuggestion.class))).thenReturn(mockResponse2);

                LocationSuggestion result2 = mapService.reverseGeocode(southPoleLat, southPoleLon);
                assertNotNull(result2);
                assertEquals("-90.0", result2.getLatitude());
        }
}
