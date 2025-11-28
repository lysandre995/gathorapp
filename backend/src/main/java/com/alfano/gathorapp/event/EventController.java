package com.alfano.gathorapp.event;

import com.alfano.gathorapp.event.dto.CreateEventRequest;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.event.dto.UpdateEventRequest;
import com.alfano.gathorapp.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for event management.
 * 
 * Endpoints:
 * - GET /api/events → List all events
 * - GET /api/events/upcoming → List upcoming events
 * - GET /api/events/{id} → Get event details
 * - GET /api/events/my → Get events created by current user
 * - POST /api/events → Create new event (BUSINESS only)
 * - PUT /api/events/{id} → Update event (creator only)
 * - DELETE /api/events/{id} → Delete event (creator only)
 */
@Tag(name = "Events", description = "Event management APIs (BUSINESS users)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    /**
     * GET /api/events
     * Get all events.
     */
    @Operation(summary = "Get all events")
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        log.info("GET /api/events - Fetching all events");
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/upcoming
     * Get all upcoming events.
     */
    @Operation(summary = "Get upcoming events")
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        log.info("GET /api/events/upcoming - Fetching upcoming events");
        List<EventResponse> events = eventService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/{id}
     * Get event by ID.
     */
    @Operation(summary = "Get event by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable("id") UUID id) {
        log.info("GET /api/events/{} - Fetching event details", id);
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * GET /api/events/my
     * Get all events created by the authenticated user.
     */
    @Operation(summary = "Get my events", description = "Get events created by authenticated user")
    @GetMapping("/my")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/events/my - Fetching events for user: {}", userId);
        List<EventResponse> events = eventService.getEventsByCreator(userId);
        return ResponseEntity.ok(events);
    }

    /**
     * POST /api/events
     * Create a new event (BUSINESS users only).
     */
    @Operation(summary = "Create event", description = "BUSINESS users only")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/events - Creating event for user: {}", userId);
        EventResponse event = eventService.createEvent(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * PUT /api/events/{id}
     * Update an event (creator only).
     */
    @Operation(summary = "Update event", description = "Only event creator can update")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("PUT /api/events/{} - Updating event for user: {}", id, userId);
        EventResponse event = eventService.updateEvent(id, request, userId);
        return ResponseEntity.ok(event);
    }

    /**
     * DELETE /api/events/{id}
     * Delete an event (creator only).
     */
    @Operation(summary = "Delete event", description = "Only event creator can delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("DELETE /api/events/{} - Deleting event for user: {}", id, userId);
        eventService.deleteEvent(id, userId);
        return ResponseEntity.noContent().build();
    }
}
