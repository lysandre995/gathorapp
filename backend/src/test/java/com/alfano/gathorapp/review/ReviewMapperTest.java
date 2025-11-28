package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.review.dto.ReviewResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReviewMapper.
 */
@DisplayName("ReviewMapper Tests")
class ReviewMapperTest {

    private ReviewMapper reviewMapper;

    private UUID reviewId;
    private UUID reviewerId;
    private UUID eventId;
    private UUID outingId;
    private User reviewer;
    private Event event;
    private Outing outing;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewMapper = new ReviewMapper();

        reviewId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        outingId = UUID.randomUUID();

        reviewer = User.builder()
                .id(reviewerId)
                .name("Test Reviewer")
                .email("reviewer@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        review = Review.builder()
                .id(reviewId)
                .reviewer(reviewer)
                .event(event)
                .rating(5)
                .comment("Excellent event!")
                .createdAt(LocalDateTime.of(2024, 12, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("toResponse - Should map review for event correctly")
    void toResponse_EventReview_MapsAllFieldsCorrectly() {
        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertNotNull(response);
        assertEquals(reviewId, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent event!", response.getComment());
        assertEquals(LocalDateTime.of(2024, 12, 1, 10, 0), response.getCreatedAt());

        // Verify reviewer info
        assertNotNull(response.getReviewer());
        assertEquals(reviewerId, response.getReviewer().getId());
        assertEquals("Test Reviewer", response.getReviewer().getName());

        // Verify event/outing IDs
        assertEquals(eventId, response.getEventId());
        assertNull(response.getOutingId());
    }

    @Test
    @DisplayName("toResponse - Should map review for outing correctly")
    void toResponse_OutingReview_MapsAllFieldsCorrectly() {
        // Given
        review.setEvent(null);
        review.setOuting(outing);

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertNotNull(response);
        assertEquals(reviewId, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent event!", response.getComment());

        // Verify reviewer info
        assertEquals(reviewerId, response.getReviewer().getId());
        assertEquals("Test Reviewer", response.getReviewer().getName());

        // Verify event/outing IDs
        assertNull(response.getEventId());
        assertEquals(outingId, response.getOutingId());
    }

    @Test
    @DisplayName("toResponse - Should map different ratings correctly")
    void toResponse_DifferentRatings_MapsDifferentRatingsCorrectly() {
        // Test rating 1
        review.setRating(1);
        ReviewResponse response1 = reviewMapper.toResponse(review);
        assertEquals(1, response1.getRating());

        // Test rating 3
        review.setRating(3);
        ReviewResponse response3 = reviewMapper.toResponse(review);
        assertEquals(3, response3.getRating());

        // Test rating 5
        review.setRating(5);
        ReviewResponse response5 = reviewMapper.toResponse(review);
        assertEquals(5, response5.getRating());
    }

    @Test
    @DisplayName("toResponse - Should map different reviewers correctly")
    void toResponse_DifferentReviewers_MapsDifferentReviewersCorrectly() {
        // Given
        User reviewer2 = User.builder()
                .id(UUID.randomUUID())
                .name("Another Reviewer")
                .email("another@example.com")
                .passwordHash("pass")
                .role(Role.USER)
                .build();

        review.setReviewer(reviewer2);

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertNotNull(response);
        assertEquals(reviewer2.getId(), response.getReviewer().getId());
        assertEquals("Another Reviewer", response.getReviewer().getName());
    }

    @Test
    @DisplayName("toResponse - Should map different comments correctly")
    void toResponse_DifferentComments_MapsDifferentCommentsCorrectly() {
        // Given
        review.setComment("Amazing experience!");

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertEquals("Amazing experience!", response.getComment());
    }

    @Test
    @DisplayName("toResponse - Should handle null comment")
    void toResponse_NullComment_MapsNullCorrectly() {
        // Given
        review.setComment(null);

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertNull(response.getComment());
    }

    @Test
    @DisplayName("toResponse - Should map different timestamps correctly")
    void toResponse_DifferentTimestamps_MapsTimestampsCorrectly() {
        // Given
        LocalDateTime customTime = LocalDateTime.of(2023, 6, 15, 14, 30);
        review.setCreatedAt(customTime);

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertEquals(customTime, response.getCreatedAt());
    }

    @Test
    @DisplayName("toResponse - Should map multiple reviews independently")
    void toResponse_MultipleReviews_MapsEachIndependently() {
        // Given
        Review review2 = Review.builder()
                .id(UUID.randomUUID())
                .reviewer(User.builder()
                        .id(UUID.randomUUID())
                        .name("Reviewer 2")
                        .email("reviewer2@example.com")
                        .passwordHash("pass")
                        .role(Role.USER)
                        .build())
                .outing(outing)
                .rating(3)
                .comment("Good outing")
                .createdAt(LocalDateTime.of(2024, 11, 20, 9, 0))
                .build();

        // When
        ReviewResponse response1 = reviewMapper.toResponse(review);
        ReviewResponse response2 = reviewMapper.toResponse(review2);

        // Then
        assertNotNull(response1);
        assertNotNull(response2);

        // Verify they are independent
        assertNotEquals(response1.getId(), response2.getId());
        assertNotEquals(response1.getReviewer().getId(), response2.getReviewer().getId());
        assertEquals(5, response1.getRating());
        assertEquals(3, response2.getRating());
        assertEquals("Excellent event!", response1.getComment());
        assertEquals("Good outing", response2.getComment());
    }

    @Test
    @DisplayName("toResponse - Should handle both event and outing null")
    void toResponse_BothEventAndOutingNull_MapsNullIds() {
        // Given
        review.setEvent(null);
        review.setOuting(null);

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertNotNull(response);
        assertNull(response.getEventId());
        assertNull(response.getOutingId());
    }

    @Test
    @DisplayName("toResponse - Should map review with minimum rating")
    void toResponse_MinimumRating_MapsCorrectly() {
        // Given
        review.setRating(1);
        review.setComment("Disappointing");

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertEquals(1, response.getRating());
        assertEquals("Disappointing", response.getComment());
    }

    @Test
    @DisplayName("toResponse - Should map review with maximum rating")
    void toResponse_MaximumRating_MapsCorrectly() {
        // Given
        review.setRating(5);
        review.setComment("Perfect!");

        // When
        ReviewResponse response = reviewMapper.toResponse(review);

        // Then
        assertEquals(5, response.getRating());
        assertEquals("Perfect!", response.getComment());
    }
}
