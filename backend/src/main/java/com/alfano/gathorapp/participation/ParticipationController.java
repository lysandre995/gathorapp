package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import com.alfano.gathorapp.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for participation management.
 * 
 * Endpoints:
 * - GET /api/participations/outing/{outingId} → Get participations for an
 * outing
 * - GET /api/participations/my → Get current user's participations
 * - POST /api/participations/outing/{outingId} → Join an outing
 * - PUT /api/participations/{id}/approve → Approve participation (organizer)
 * - PUT /api/participations/{id}/reject → Reject participation (organizer)
 * - DELETE /api/participations/{id} → Leave outing
 */
@Tag(name = "Participations", description = "Participation management APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/participations")
@RequiredArgsConstructor
@Slf4j
public class ParticipationController {

    private final ParticipationService participationService;

    /**
     * GET /api/participations/outing/{outingId}
     * Get all participations for a specific outing.
     */
    @Operation(summary = "Get participations for an outing")
    @GetMapping("/outing/{outingId}")
    public ResponseEntity<List<ParticipationResponse>> getParticipationsByOuting(
            @PathVariable("outingId") UUID outingId) {
        log.info("GET /api/participations/outing/{} - Fetching participations", outingId);
        List<ParticipationResponse> participations = participationService.getParticipationsByOuting(outingId);
        return ResponseEntity.ok(participations);
    }

    /**
     * GET /api/participations/my
     * Get all participations by the authenticated user.
     */
    @Operation(summary = "Get my participations")
    @GetMapping("/my")
    public ResponseEntity<List<ParticipationResponse>> getMyParticipations(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/participations/my - Fetching participations for user: {}", userId);
        List<ParticipationResponse> participations = participationService.getParticipationsByUser(userId);
        return ResponseEntity.ok(participations);
    }

    /**
     * POST /api/participations/outing/{outingId}
     * Request to join an outing.
     */
    @Operation(summary = "Join an outing", description = "Request participation in an outing. Subject to maximum participants limit.")
    @PostMapping("/outing/{outingId}")
    public ResponseEntity<ParticipationResponse> joinOuting(
            @PathVariable("outingId") UUID outingId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/participations/outing/{} - User {} joining", outingId, userId);
        ParticipationResponse participation = participationService.joinOuting(outingId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participation);
    }

    /**
     * PUT /api/participations/{id}/approve
     * Approve a participation request (organizer only).
     */
    @Operation(summary = "Approve participation", description = "Approve a pending participation request. Only organizer can approve.")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ParticipationResponse> approveParticipation(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("PUT /api/participations/{}/approve - Organizer {} approving", id, userId);
        ParticipationResponse participation = participationService.approveParticipation(id, userId);
        return ResponseEntity.ok(participation);
    }

    /**
     * PUT /api/participations/{id}/reject
     * Reject a participation request (organizer only).
     */
    @Operation(summary = "Reject participation", description = "Reject a pending participation request. Only organizer can reject.")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ParticipationResponse> rejectParticipation(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("PUT /api/participations/{}/reject - Organizer {} rejecting", id, userId);
        ParticipationResponse participation = participationService.rejectParticipation(id, userId);
        return ResponseEntity.ok(participation);
    }

    /**
     * DELETE /api/participations/{id}
     * Leave an outing (cancel own participation).
     */
    @Operation(summary = "Leave outing", description = "Cancel own participation in an outing")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> leaveOuting(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("DELETE /api/participations/{} - User {} leaving", id, userId);
        participationService.leaveOuting(id, userId);
        return ResponseEntity.noContent().build();
    }
}
