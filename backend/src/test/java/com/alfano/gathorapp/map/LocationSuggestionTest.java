package com.alfano.gathorapp.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocationSuggestion DTO and its methods.
 */
@DisplayName("LocationSuggestion Tests")
class LocationSuggestionTest {

    private LocationSuggestion locationSuggestion;
    private LocationSuggestion.AddressComponents addressComponents;

    @BeforeEach
    void setUp() {
        addressComponents = LocationSuggestion.AddressComponents.builder()
                .road("Via Roma")
                .city("Milan")
                .state("Lombardy")
                .country("Italy")
                .postCode("20121")
                .countryCode("IT")
                .build();

        locationSuggestion = LocationSuggestion.builder()
                .displayName("Via Roma, Milan, Lombardy, Italy")
                .latitude("45.4642")
                .longitude("9.1900")
                .type("road")
                .importance(0.8)
                .address(addressComponents)
                .build();
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should convert latitude string to double")
    void getLatitudeAsDouble_ValidLatitude_ConvertsToDouble() {
        // When
        Double lat = locationSuggestion.getLatitudeAsDouble();

        // Then
        assertNotNull(lat);
        assertEquals(45.4642, lat, 0.0001);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should convert longitude string to double")
    void getLongitudeAsDouble_ValidLongitude_ConvertsToDouble() {
        // When
        Double lon = locationSuggestion.getLongitudeAsDouble();

        // Then
        assertNotNull(lon);
        assertEquals(9.1900, lon, 0.0001);
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should return null when latitude is null")
    void getLatitudeAsDouble_NullLatitude_ReturnsNull() {
        // Given
        locationSuggestion.setLatitude(null);

        // When
        Double lat = locationSuggestion.getLatitudeAsDouble();

        // Then
        assertNull(lat);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should return null when longitude is null")
    void getLongitudeAsDouble_NullLongitude_ReturnsNull() {
        // Given
        locationSuggestion.setLongitude(null);

        // When
        Double lon = locationSuggestion.getLongitudeAsDouble();

        // Then
        assertNull(lon);
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should handle negative latitude")
    void getLatitudeAsDouble_NegativeLatitude_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLatitude("-33.8688");

        // When
        Double lat = locationSuggestion.getLatitudeAsDouble();

        // Then
        assertNotNull(lat);
        assertEquals(-33.8688, lat, 0.0001);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should handle negative longitude")
    void getLongitudeAsDouble_NegativeLongitude_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLongitude("-118.4085");

        // When
        Double lon = locationSuggestion.getLongitudeAsDouble();

        // Then
        assertNotNull(lon);
        assertEquals(-118.4085, lon, 0.0001);
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should handle zero latitude")
    void getLatitudeAsDouble_ZeroLatitude_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLatitude("0.0");

        // When
        Double lat = locationSuggestion.getLatitudeAsDouble();

        // Then
        assertNotNull(lat);
        assertEquals(0.0, lat, 0.0001);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should handle zero longitude")
    void getLongitudeAsDouble_ZeroLongitude_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLongitude("0.0");

        // When
        Double lon = locationSuggestion.getLongitudeAsDouble();

        // Then
        assertNotNull(lon);
        assertEquals(0.0, lon, 0.0001);
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should handle high precision latitude")
    void getLatitudeAsDouble_HighPrecision_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLatitude("45.464203892847");

        // When
        Double lat = locationSuggestion.getLatitudeAsDouble();

        // Then
        assertNotNull(lat);
        assertEquals(45.464203892847, lat, 0.000001);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should handle high precision longitude")
    void getLongitudeAsDouble_HighPrecision_ConvertsCorrectly() {
        // Given
        locationSuggestion.setLongitude("9.190019384756");

        // When
        Double lon = locationSuggestion.getLongitudeAsDouble();

        // Then
        assertNotNull(lon);
        assertEquals(9.190019384756, lon, 0.000001);
    }

    @Test
    @DisplayName("Builder - Should build LocationSuggestion with all fields")
    void builder_AllFields_BuildsCorrectly() {
        // Given / When
        LocationSuggestion suggestion = LocationSuggestion.builder()
                .displayName("Test Location")
                .latitude("40.7128")
                .longitude("-74.0060")
                .type("city")
                .importance(0.9)
                .address(addressComponents)
                .build();

        // Then
        assertNotNull(suggestion);
        assertEquals("Test Location", suggestion.getDisplayName());
        assertEquals("40.7128", suggestion.getLatitude());
        assertEquals("-74.0060", suggestion.getLongitude());
        assertEquals("city", suggestion.getType());
        assertEquals(0.9, suggestion.getImportance());
        assertNotNull(suggestion.getAddress());
    }

    @Test
    @DisplayName("Builder - Should build LocationSuggestion with minimal fields")
    void builder_MinimalFields_BuildsCorrectly() {
        // Given / When
        LocationSuggestion suggestion = LocationSuggestion.builder()
                .displayName("Simple Location")
                .latitude("51.5074")
                .longitude("-0.1278")
                .build();

        // Then
        assertNotNull(suggestion);
        assertEquals("Simple Location", suggestion.getDisplayName());
        assertEquals("51.5074", suggestion.getLatitude());
        assertEquals("-0.1278", suggestion.getLongitude());
        assertNull(suggestion.getType());
        assertNull(suggestion.getImportance());
        assertNull(suggestion.getAddress());
    }

    @Test
    @DisplayName("AddressComponents Builder - Should build with all fields")
    void addressComponentsBuilder_AllFields_BuildsCorrectly() {
        // Given / When
        LocationSuggestion.AddressComponents address = LocationSuggestion.AddressComponents.builder()
                .road("Main Street")
                .city("New York")
                .state("NY")
                .country("USA")
                .postCode("10001")
                .countryCode("US")
                .build();

        // Then
        assertNotNull(address);
        assertEquals("Main Street", address.getRoad());
        assertEquals("New York", address.getCity());
        assertEquals("NY", address.getState());
        assertEquals("USA", address.getCountry());
        assertEquals("10001", address.getPostCode());
        assertEquals("US", address.getCountryCode());
    }

    @Test
    @DisplayName("AddressComponents Builder - Should build with minimal fields")
    void addressComponentsBuilder_MinimalFields_BuildsCorrectly() {
        // Given / When
        LocationSuggestion.AddressComponents address = LocationSuggestion.AddressComponents.builder()
                .city("Rome")
                .country("Italy")
                .build();

        // Then
        assertNotNull(address);
        assertNull(address.getRoad());
        assertEquals("Rome", address.getCity());
        assertNull(address.getState());
        assertEquals("Italy", address.getCountry());
        assertNull(address.getPostCode());
        assertNull(address.getCountryCode());
    }

    @Test
    @DisplayName("Setters and Getters - Should work correctly for all fields")
    void settersAndGetters_AllFields_WorkCorrectly() {
        // Given
        LocationSuggestion suggestion = new LocationSuggestion();

        // When
        suggestion.setDisplayName("Test Display Name");
        suggestion.setLatitude("48.8566");
        suggestion.setLongitude("2.3522");
        suggestion.setType("monument");
        suggestion.setImportance(0.95);
        suggestion.setAddress(addressComponents);

        // Then
        assertEquals("Test Display Name", suggestion.getDisplayName());
        assertEquals("48.8566", suggestion.getLatitude());
        assertEquals("2.3522", suggestion.getLongitude());
        assertEquals("monument", suggestion.getType());
        assertEquals(0.95, suggestion.getImportance());
        assertEquals(addressComponents, suggestion.getAddress());
    }

    @Test
    @DisplayName("Different LocationSuggestions - Should be independent")
    void multipleLocationSuggestions_AreIndependent() {
        // Given
        LocationSuggestion suggestion1 = LocationSuggestion.builder()
                .displayName("Location 1")
                .latitude("10.0")
                .longitude("20.0")
                .build();

        LocationSuggestion suggestion2 = LocationSuggestion.builder()
                .displayName("Location 2")
                .latitude("30.0")
                .longitude("40.0")
                .build();

        // When / Then
        assertNotEquals(suggestion1.getDisplayName(), suggestion2.getDisplayName());
        assertNotEquals(suggestion1.getLatitude(), suggestion2.getLatitude());
        assertNotEquals(suggestion1.getLongitude(), suggestion2.getLongitude());
    }

    @Test
    @DisplayName("getLatitudeAsDouble - Should handle extreme latitude values")
    void getLatitudeAsDouble_ExtremeValues_ConvertsCorrectly() {
        // Test max latitude
        locationSuggestion.setLatitude("90.0");
        assertEquals(90.0, locationSuggestion.getLatitudeAsDouble(), 0.0001);

        // Test min latitude
        locationSuggestion.setLatitude("-90.0");
        assertEquals(-90.0, locationSuggestion.getLatitudeAsDouble(), 0.0001);
    }

    @Test
    @DisplayName("getLongitudeAsDouble - Should handle extreme longitude values")
    void getLongitudeAsDouble_ExtremeValues_ConvertsCorrectly() {
        // Test max longitude
        locationSuggestion.setLongitude("180.0");
        assertEquals(180.0, locationSuggestion.getLongitudeAsDouble(), 0.0001);

        // Test min longitude
        locationSuggestion.setLongitude("-180.0");
        assertEquals(-180.0, locationSuggestion.getLongitudeAsDouble(), 0.0001);
    }

    @Test
    @DisplayName("Importance values - Should handle different importance scores")
    void importance_DifferentValues_HandledCorrectly() {
        // Test minimum importance
        locationSuggestion.setImportance(0.0);
        assertEquals(0.0, locationSuggestion.getImportance());

        // Test maximum importance
        locationSuggestion.setImportance(1.0);
        assertEquals(1.0, locationSuggestion.getImportance());

        // Test medium importance
        locationSuggestion.setImportance(0.5);
        assertEquals(0.5, locationSuggestion.getImportance());
    }

    @Test
    @DisplayName("Type field - Should handle different location types")
    void type_DifferentTypes_HandledCorrectly() {
        // Test different types
        String[] types = {"road", "city", "amenity", "building", "monument", "park"};

        for (String type : types) {
            locationSuggestion.setType(type);
            assertEquals(type, locationSuggestion.getType());
        }
    }
}
