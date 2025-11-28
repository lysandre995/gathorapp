package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.review.dto.CreateReviewRequest;
import com.alfano.gathorapp.review.dto.ReviewResponse;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing reviews.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final OutingRepository outingRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Get all reviews for an event.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByEvent(UUID eventId) {
        log.debug("Fetching reviews for event: {}", eventId);
        return reviewRepository.findByEventId(eventId)
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all reviews for an outing.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByOuting(UUID outingId) {
        log.debug("Fetching reviews for outing: {}", outingId);
        return reviewRepository.findByOutingId(outingId)
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all reviews by a user.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(UUID userId) {
        log.debug("Fetching reviews by user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return reviewRepository.findByReviewer(user)
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get average rating for an outing.
     */
    @Transactional(readOnly = true)
    public Double getAverageRatingForOuting(UUID outingId) {
        log.debug("Calculating average rating for outing: {}", outingId);
        List<Review> reviews = reviewRepository.findByOutingId(outingId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Create a review.
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, UUID userId) {
        log.info("User {} creating review", userId);

        // Validate that either event or outing is provided
        if ((request.getEventId() == null && request.getOutingId() == null) ||
                (request.getEventId() != null && request.getOutingId() != null)) {
            throw new RuntimeException("Review must be for either an event or an outing, not both");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = null;
        Outing outing = null;

        // Handle event review
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Check if user already reviewed this event
            if (reviewRepository.existsByReviewerAndEvent(user, event)) {
                throw new DuplicateReviewException("You have already reviewed this event");
            }
        }

        // Handle outing review
        if (request.getOutingId() != null) {
            outing = outingRepository.findById(request.getOutingId())
                    .orElseThrow(() -> new RuntimeException("Outing not found"));

            // Check if user already reviewed this outing
            if (reviewRepository.existsByReviewerAndOuting(user, outing)) {
                throw new DuplicateReviewException("You have already reviewed this outing");
            }

            // Verify user participated in outing
            if (!participationRepository.existsByUserAndOuting(user, outing)) {
                throw new UnauthorizedReviewAccessException("You must participate in the outing to review it");
            }
        }

        // Create review
        Review review = Review.builder()
                .reviewer(user)
                .event(event)
                .outing(outing)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created: {}", savedReview.getId());

        return reviewMapper.toResponse(savedReview);
    }
}
