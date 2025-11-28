package com.alfano.gathorapp.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a location suggestion from geocoding services.
 * Used for both forward geocoding (address -> coordinates) and
 * reverse geocoding (coordinates -> address).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSuggestion {

    /**
     * Display name / formatted address.
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * Latitude coordinate.
     */
    @JsonProperty("lat")
    private String latitude;

    /**
     * Longitude coordinate.
     */
    @JsonProperty("lon")
    private String longitude;

    /**
     * Type of location (e.g., "city", "road", "amenity").
     */
    private String type;

    /**
     * Importance/relevance score (0.0 to 1.0).
     */
    private Double importance;

    /**
     * Additional address components.
     */
    private AddressComponents address;

    /**
     * Nested DTO for detailed address components.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressComponents {
        private String road;
        private String city;
        private String state;
        private String country;

        @JsonProperty("postcode")
        private String postCode;

        @JsonProperty("country_code")
        private String countryCode;
    }

    /**
     * Convert latitude string to double.
     */
    public Double getLatitudeAsDouble() {
        return latitude != null ? Double.parseDouble(latitude) : null;
    }

    /**
     * Convert longitude string to double.
     */
    public Double getLongitudeAsDouble() {
        return longitude != null ? Double.parseDouble(longitude) : null;
    }
}
