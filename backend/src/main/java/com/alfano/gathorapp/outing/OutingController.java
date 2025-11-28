package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
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
 * REST Controller for outing management.
 * 
 * Endpoints:
 * - GET /api/outings → List all outings
 * - GET /api/outings/upcoming → List upcoming outings
 * - GET /api/outings/{id} → Get outing details
 * - GET /api/outings/my → Get outings organized by current user
 * - GET /api/outings/event/{id} → Get outings for a specific event
 * - POST /api/outings → Create new outing
 * - DELETE /api/outings/{id} → Delete outing (organizer only)
 */
@Tag(name = "Outings", description = "Outing management APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/outings")
@RequiredArgsConstructor
@Slf4j
public class OutingController {

    private final OutingService outingService;

    /**
     * GET /api/outings
     * Get all outings.
     */
    @Operation(summary = "Get all outings")
    @GetMapping
    public ResponseEntity<List<OutingResponse>> getAllOutings() {
        log.info("GET /api/outings - Fetching all outings");
        List<OutingResponse> outings = outingService.getAllOutings();
        return ResponseEntity.ok(outings);
    }

    /**
     * GET /api/outings/upcoming
     * Get all upcoming outings.
     */
    @Operation(summary = "Get upcoming outings")
    @GetMapping("/upcoming")
    public ResponseEntity<List<OutingResponse>> getUpcomingOutings() {
        log.info("GET /api/outings/upcoming - Fetching upcoming outings");
        List<OutingResponse> outings = outingService.getUpcomingOutings();
        return ResponseEntity.ok(outings);
    }

    /**
     * GET /api/outings/{id}
     * Get outing by ID.
     */
    @Operation(summary = "Get outing by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OutingResponse> getOutingById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser != null ? securityUser.getUserId() : null;
        log.info("GET /api/outings/{} - Fetching outing details", id);
        OutingResponse outing = outingService.getOutingById(id, userId);
        return ResponseEntity.ok(outing);
    }

    /**
     * GET /api/outings/my
     * Get all outings organized by the authenticated user.
     */
    @Operation(summary = "Get my outings", description = "Get outings organized by authenticated user")
    @GetMapping("/my")
    public ResponseEntity<List<OutingResponse>> getMyOutings(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/outings/my - Fetching outings for user: {}", userId);
        List<OutingResponse> outings = outingService.getOutingsByOrganizer(userId);
        return ResponseEntity.ok(outings);
    }

    /**
     * GET /api/outings/event/{eventId}
     * Get all outings linked to a specific event.
     */
    @Operation(summary = "Get outings for an event")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<OutingResponse>> getOutingsByEvent(@PathVariable("eventId") UUID eventId) {
        log.info("GET /api/outings/event/{} - Fetching outings for event", eventId);
        List<OutingResponse> outings = outingService.getOutingsByEvent(eventId);
        return ResponseEntity.ok(outings);
    }

    /**
     * POST /api/outings
     * Create a new outing.
     */
    @Operation(summary = "Create outing", description = "Subject to user role limitations")
    @PostMapping
    public ResponseEntity<OutingResponse> createOuting(
            @Valid @RequestBody CreateOutingRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/outings - Creating outing for user: {}", userId);
        OutingResponse outing = outingService.createOuting(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(outing);
    }

    /**
     * DELETE /api/outings/{id}
     * Delete an outing (organizer only).
     */
    @Operation(summary = "Delete outing", description = "Only organizer can delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOuting(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("DELETE /api/outings/{} - Deleting outing for user: {}", id, userId);
        outingService.deleteOuting(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/outings/{id}/join
     * Join an outing as a participant.
     */
    @Operation(summary = "Join outing", description = "Authenticated user joins an outing")
    @PostMapping("/{id}/join")
    public ResponseEntity<OutingResponse> joinOuting(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/outings/{}/join - User {} joining outing", id, userId);
        OutingResponse outing = outingService.joinOuting(id, userId);
        return ResponseEntity.ok(outing);
    }

    /**
     * POST /api/outings/{id}/leave
     * Leave an outing.
     */
    @Operation(summary = "Leave outing", description = "Authenticated user leaves an outing")
    @PostMapping("/{id}/leave")
    public ResponseEntity<OutingResponse> leaveOuting(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/outings/{}/leave - User {} leaving outing", id, userId);
        OutingResponse outing = outingService.leaveOuting(id, userId);
        return ResponseEntity.ok(outing);
    }
}
