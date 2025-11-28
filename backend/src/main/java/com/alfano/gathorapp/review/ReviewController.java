package com.alfano.gathorapp.review;

import com.alfano.gathorapp.review.dto.CreateReviewRequest;
import com.alfano.gathorapp.review.dto.ReviewResponse;
import com.alfano.gathorapp.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
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
 * REST Controller for review management.
 * 
 * Endpoints:
 * - GET /api/reviews/event/{eventId} → Get reviews for an event
 * - GET /api/reviews/outing/{outingId} → Get reviews for an outing
 * - POST /api/reviews → Create a review
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * GET /api/reviews/event/{eventId}
     * Get all reviews for an event.
     */
    @Operation(summary = "Get reviews for an event")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByEvent(@PathVariable("eventId") UUID eventId) {
        log.info("GET /api/reviews/event/{} - Fetching reviews", eventId);
        List<ReviewResponse> reviews = reviewService.getReviewsByEvent(eventId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * GET /api/reviews/outing/{outingId}
     * Get all reviews for an outing.
     */
    @Operation(summary = "Get reviews for an outing")
    @GetMapping("/outing/{outingId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByOuting(@PathVariable("outingId") UUID outingId) {
        log.info("GET /api/reviews/outing/{} - Fetching reviews", outingId);
        List<ReviewResponse> reviews = reviewService.getReviewsByOuting(outingId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * POST /api/reviews
     * Create a new review.
     */
    @Operation(summary = "Create review", description = "Create a review for an event or outing. Must have participated.")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/reviews - Creating review for user: {}", userId);
        ReviewResponse review = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * POST /api/reviews/outings/{outingId}
     * Create a new review for an outing.
     */
    @Operation(summary = "Create review for outing", description = "Create a review for an outing. Must have participated.")
    @PostMapping("/outings/{outingId}")
    public ResponseEntity<ReviewResponse> createReviewForOuting(
            @PathVariable("outingId") UUID outingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/reviews/outings/{} - Creating review for user: {}", outingId, userId);
        request.setOutingId(outingId);
        ReviewResponse review = reviewService.createReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * GET /api/reviews/users/{userId}
     * Get all reviews by a user.
     */
    @Operation(summary = "Get reviews by user")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByUser(@PathVariable("userId") UUID userId) {
        log.info("GET /api/reviews/users/{} - Fetching reviews by user", userId);
        List<ReviewResponse> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * GET /api/reviews/outings/{outingId}/average
     * Get average rating for an outing.
     */
    @Operation(summary = "Get average rating for outing")
    @GetMapping("/outings/{outingId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable("outingId") UUID outingId) {
        log.info("GET /api/reviews/outings/{}/average - Fetching average rating", outingId);
        Double averageRating = reviewService.getAverageRatingForOuting(outingId);
        return ResponseEntity.ok(averageRating);
    }
}
