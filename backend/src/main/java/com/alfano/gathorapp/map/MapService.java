package com.alfano.gathorapp.map;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventMapper;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingMapper;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for geolocation and map-related operations.
 *
 * Provides proximity search using the Haversine formula for calculating
 * distances between coordinates, and integrates with OpenStreetMap Nominatim
 * API for geocoding services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapService {

    private final EventRepository eventRepository;
    private final OutingRepository outingRepository;
    private final EventMapper eventMapper;
    private final OutingMapper outingMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * OpenStreetMap Nominatim API base URL.
     */
    private static final String NOMINATIM_API = "https://nominatim.openstreetmap.org";

    /**
     * Earth's radius in kilometers.
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Find nearby events within a specified radius.
     *
     * @param latitude  User's latitude
     * @param longitude User's longitude
     * @param radiusKm  Search radius in kilometers
     * @param limit     Maximum number of results
     * @return List of nearby events sorted by distance
     */
    @Transactional(readOnly = true)
    public List<EventResponse> findNearbyEvents(Double latitude, Double longitude, Double radiusKm, Integer limit) {
        log.debug("Finding events within {}km of ({}, {})", radiusKm, latitude, longitude);

        // Get all upcoming events
        List<Event> allEvents = eventRepository.findByEventDateAfter(LocalDateTime.now());

        // Filter by distance and sort
        return allEvents.stream()
                .map(event -> {
                    double distance = calculateDistance(latitude, longitude,
                            event.getLatitude(), event.getLongitude());
                    return new EventWithDistance(event, distance);
                })
                .filter(ewd -> ewd.distance <= radiusKm)
                .sorted(Comparator.comparingDouble(ewd -> ewd.distance))
                .limit(limit)
                .map(ewd -> eventMapper.toResponse(ewd.event))
                .collect(Collectors.toList());
    }

    /**
     * Find nearby outings within a specified radius.
     *
     * @param latitude  User's latitude
     * @param longitude User's longitude
     * @param radiusKm  Search radius in kilometers
     * @param limit     Maximum number of results
     * @return List of nearby outings sorted by distance
     */
    @Transactional(readOnly = true)
    public List<OutingResponse> findNearbyOutings(Double latitude, Double longitude, Double radiusKm, Integer limit) {
        log.debug("Finding outings within {}km of ({}, {})", radiusKm, latitude, longitude);

        // Get all upcoming outings
        List<Outing> allOutings = outingRepository.findByOutingDateAfter(LocalDateTime.now());

        // Filter by distance and sort
        return allOutings.stream()
                .map(outing -> {
                    double distance = calculateDistance(latitude, longitude,
                            outing.getLatitude(), outing.getLongitude());
                    return new OutingWithDistance(outing, distance);
                })
                .filter(owd -> owd.distance <= radiusKm)
                .sorted(Comparator.comparingDouble(owd -> owd.distance))
                .limit(limit)
                .map(owd -> outingMapper.toResponse(owd.outing))
                .collect(Collectors.toList());
    }

    /**
     * Geocode: Convert address/place name to coordinates.
     * Uses OpenStreetMap Nominatim API.
     *
     * @param query Search query (address or place name)
     * @param limit Maximum number of results
     * @return List of location suggestions with coordinates
     */
    public List<LocationSuggestion> geocode(String query, Integer limit) {
        log.debug("Geocoding query: '{}'", query);

        try {
            String url = UriComponentsBuilder
                    .fromUri(URI.create(NOMINATIM_API + "/search"))
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", limit)
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            ResponseEntity<List<LocationSuggestion>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LocationSuggestion>>() {
                    });

            List<LocationSuggestion> suggestions = response.getBody();
            log.info("Geocoding returned {} results for query '{}'",
                    suggestions != null ? suggestions.size() : 0, query);

            return suggestions != null ? suggestions : new ArrayList<>();

        } catch (Exception e) {
            log.error("Error geocoding query '{}': {}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Reverse geocode: Convert coordinates to address.
     * Uses OpenStreetMap Nominatim API.
     *
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Location information with formatted address
     */
    public LocationSuggestion reverseGeocode(Double latitude, Double longitude) {
        log.debug("Reverse geocoding: ({}, {})", latitude, longitude);

        try {
            String url = UriComponentsBuilder.fromUri(URI.create(NOMINATIM_API + "/reverse"))
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            ResponseEntity<LocationSuggestion> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    LocationSuggestion.class);

            LocationSuggestion location = response.getBody();
            log.info("Reverse geocoding found: {}",
                    location != null ? location.getDisplayName() : "null");

            return location;

        } catch (Exception e) {
            log.error("Error reverse geocoding ({}, {}): {}", latitude, longitude, e.getMessage());

            // Return a basic location with coordinates
            return LocationSuggestion.builder()
                    .latitude(String.valueOf(latitude))
                    .longitude(String.valueOf(longitude))
                    .displayName(String.format("Location at %.6f, %.6f", latitude, longitude))
                    .build();
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Helper record for tracking events with their distances.
     */
    private record EventWithDistance(Event event, double distance) {
    }

    /**
     * Helper record for tracking outings with their distances.
     */
    private record OutingWithDistance(Outing outing, double distance) {
    }
}
