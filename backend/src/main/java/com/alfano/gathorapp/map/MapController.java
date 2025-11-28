package com.alfano.gathorapp.map;

import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for map and geolocation features.
 *
 * Provides endpoints for:
 * - Finding nearby events based on coordinates
 * - Finding nearby outings based on coordinates
 * - Location autocomplete (future integration with OpenStreetMap Nominatim API)
 */
@Tag(name = "Map", description = "Geolocation and map APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Slf4j
public class MapController {

    private final MapService mapService;

    /**
     * GET /api/map/events/nearby
     * Find events within a specified radius from given coordinates.
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param radiusKm Search radius in kilometers (default: 10km)
     * @param limit Maximum number of results (default: 50)
     * @return List of nearby events sorted by distance
     */
    @Operation(summary = "Find nearby events",
               description = "Find events within a specified radius from given coordinates, sorted by distance")
    @GetMapping("/events/nearby")
    public ResponseEntity<List<EventResponse>> getNearbyEvents(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(name = "radiusKm", defaultValue = "10.0") Double radiusKm,
            @RequestParam(name = "limit", defaultValue = "50") Integer limit) {

        log.info("GET /api/map/events/nearby - lat: {}, lon: {}, radius: {}km, limit: {}",
                 latitude, longitude, radiusKm, limit);

        List<EventResponse> events = mapService.findNearbyEvents(latitude, longitude, radiusKm, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/map/outings/nearby
     * Find outings within a specified radius from given coordinates.
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param radiusKm Search radius in kilometers (default: 10km)
     * @param limit Maximum number of results (default: 50)
     * @return List of nearby outings sorted by distance
     */
    @Operation(summary = "Find nearby outings",
               description = "Find outings within a specified radius from given coordinates, sorted by distance")
    @GetMapping("/outings/nearby")
    public ResponseEntity<List<OutingResponse>> getNearbyOutings(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(name = "radiusKm", defaultValue = "10.0") Double radiusKm,
            @RequestParam(name = "limit", defaultValue = "50") Integer limit) {

        log.info("GET /api/map/outings/nearby - lat: {}, lon: {}, radius: {}km, limit: {}",
                 latitude, longitude, radiusKm, limit);

        List<OutingResponse> outings = mapService.findNearbyOutings(latitude, longitude, radiusKm, limit);
        return ResponseEntity.ok(outings);
    }

    /**
     * GET /api/map/geocode
     * Search for location coordinates by address/place name.
     * Uses OpenStreetMap Nominatim API for geocoding.
     *
     * @param query Search query (address or place name)
     * @param limit Maximum number of results (default: 5)
     * @return List of location suggestions with coordinates
     */
    @Operation(summary = "Geocode location",
               description = "Convert address/place name to coordinates using OpenStreetMap Nominatim")
    @GetMapping("/geocode")
    public ResponseEntity<List<LocationSuggestion>> geocode(
            @RequestParam("query") String query,
            @RequestParam(name = "limit", defaultValue = "5") Integer limit) {

        log.info("GET /api/map/geocode - query: '{}', limit: {}", query, limit);

        List<LocationSuggestion> suggestions = mapService.geocode(query, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * GET /api/map/reverse-geocode
     * Convert coordinates to a human-readable address.
     * Uses OpenStreetMap Nominatim API for reverse geocoding.
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Location information with formatted address
     */
    @Operation(summary = "Reverse geocode coordinates",
               description = "Convert coordinates to address using OpenStreetMap Nominatim")
    @GetMapping("/reverse-geocode")
    public ResponseEntity<LocationSuggestion> reverseGeocode(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude) {

        log.info("GET /api/map/reverse-geocode - lat: {}, lon: {}", latitude, longitude);

        LocationSuggestion location = mapService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(location);
    }
}
