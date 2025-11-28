package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.reward.dto.CreateRewardRequest;
import com.alfano.gathorapp.reward.dto.RewardResponse;
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
 * REST Controller for reward management.
 * 
 * Endpoints:
 * - GET /api/rewards/event/{eventId} → Get rewards for an event
 * - POST /api/rewards → Create reward (BUSINESS only)
 */
@Tag(name = "Rewards", description = "Reward management APIs (BUSINESS users)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@Slf4j
public class RewardController {

    private final RewardService rewardService;

    /**
     * GET /api/rewards/event/{eventId}
     * Get all rewards for an event.
     */
    @Operation(summary = "Get rewards for an event")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<RewardResponse>> getRewardsByEvent(@PathVariable("eventId") UUID eventId) {
        log.info("GET /api/rewards/event/{} - Fetching rewards", eventId);
        List<RewardResponse> rewards = rewardService.getRewardsByEvent(eventId);
        return ResponseEntity.ok(rewards);
    }

    /**
     * POST /api/rewards
     * Create a new reward (BUSINESS only).
     */
    @Operation(summary = "Create reward", description = "Create a reward for an event. BUSINESS users only.")
    @PostMapping
    public ResponseEntity<RewardResponse> createReward(
            @Valid @RequestBody CreateRewardRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/rewards - Creating reward for user: {}", userId);
        RewardResponse reward = rewardService.createReward(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reward);
    }
}
