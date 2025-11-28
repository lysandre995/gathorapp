package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Review entity methods and validation.
 */
@DisplayName("Review Entity Tests")
class ReviewTest {

    private User reviewer;
    private Event event;
    private Outing outing;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewer = User.builder()
                .id(UUID.randomUUID())
                .name("Test Reviewer")
                .email("reviewer@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .build();

        outing = Outing.builder()
                .id(UUID.randomUUID())
                .title("Test Outing")
                .build();

        review = Review.builder()
                .id(UUID.randomUUID())
                .reviewer(reviewer)
                .event(event)
                .rating(5)
                .comment("Great experience!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("isEventReview - Should return true when event is set")
    void isEventReview_EventSet_ReturnsTrue() {
        // Given
        review.setEvent(event);
        review.setOuting(null);

        // When
        boolean result = review.isEventReview();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("isEventReview - Should return false when event is null")
    void isEventReview_EventNull_ReturnsFalse() {
        // Given
        review.setEvent(null);
        review.setOuting(outing);

        // When
        boolean result = review.isEventReview();

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("isOutingReview - Should return true when outing is set")
    void isOutingReview_OutingSet_ReturnsTrue() {
        // Given
        review.setEvent(null);
        review.setOuting(outing);

        // When
        boolean result = review.isOutingReview();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("isOutingReview - Should return false when outing is null")
    void isOutingReview_OutingNull_ReturnsFalse() {
        // Given
        review.setEvent(event);
        review.setOuting(null);

        // When
        boolean result = review.isOutingReview();

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("onCreate - Should set createdAt timestamp")
    void onCreate_SetsCreatedAtTimestamp() {
        // Given
        Review newReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(4)
                .comment("Good event")
                .build();
        assertNull(newReview.getCreatedAt());

        // When
        newReview.onCreate();

        // Then
        assertNotNull(newReview.getCreatedAt());
    }

    @Test
    @DisplayName("onCreate - Should validate rating minimum boundary (1)")
    void onCreate_RatingMinimumBoundary_Valid() {
        // Given
        Review newReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(1)
                .comment("Could be better")
                .build();

        // When / Then
        assertDoesNotThrow(() -> newReview.onCreate());
    }

    @Test
    @DisplayName("onCreate - Should validate rating maximum boundary (5)")
    void onCreate_RatingMaximumBoundary_Valid() {
        // Given
        Review newReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(5)
                .comment("Perfect!")
                .build();

        // When / Then
        assertDoesNotThrow(() -> newReview.onCreate());
    }

    @Test
    @DisplayName("onCreate - Should throw exception when rating is less than 1")
    void onCreate_RatingTooLow_ThrowsException() {
        // Given
        Review invalidReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(0)
                .comment("Invalid")
                .build();

        // When / Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invalidReview.onCreate()
        );
        assertEquals("Rating must be between 1 and 5", exception.getMessage());
    }

    @Test
    @DisplayName("onCreate - Should throw exception when rating is greater than 5")
    void onCreate_RatingTooHigh_ThrowsException() {
        // Given
        Review invalidReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(6)
                .comment("Invalid")
                .build();

        // When / Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invalidReview.onCreate()
        );
        assertEquals("Rating must be between 1 and 5", exception.getMessage());
    }

    @Test
    @DisplayName("onCreate - Should throw exception when rating is negative")
    void onCreate_RatingNegative_ThrowsException() {
        // Given
        Review invalidReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(-1)
                .comment("Invalid")
                .build();

        // When / Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invalidReview.onCreate()
        );
        assertEquals("Rating must be between 1 and 5", exception.getMessage());
    }

    @Test
    @DisplayName("onCreate - Should throw exception when both event and outing are null")
    void onCreate_BothEventAndOutingNull_ThrowsException() {
        // Given
        Review invalidReview = Review.builder()
                .reviewer(reviewer)
                .event(null)
                .outing(null)
                .rating(5)
                .comment("Invalid")
                .build();

        // When / Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> invalidReview.onCreate()
        );
        assertEquals("Review must be for either an event or an outing, not both", exception.getMessage());
    }

    @Test
    @DisplayName("onCreate - Should throw exception when both event and outing are set")
    void onCreate_BothEventAndOutingSet_ThrowsException() {
        // Given
        Review invalidReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .outing(outing)
                .rating(5)
                .comment("Invalid")
                .build();

        // When / Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> invalidReview.onCreate()
        );
        assertEquals("Review must be for either an event or an outing, not both", exception.getMessage());
    }

    @Test
    @DisplayName("onCreate - Valid event review should not throw exception")
    void onCreate_ValidEventReview_DoesNotThrow() {
        // Given
        Review validReview = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .outing(null)
                .rating(4)
                .comment("Great event!")
                .build();

        // When / Then
        assertDoesNotThrow(() -> validReview.onCreate());
        assertNotNull(validReview.getCreatedAt());
    }

    @Test
    @DisplayName("onCreate - Valid outing review should not throw exception")
    void onCreate_ValidOutingReview_DoesNotThrow() {
        // Given
        Review validReview = Review.builder()
                .reviewer(reviewer)
                .event(null)
                .outing(outing)
                .rating(3)
                .comment("Good outing!")
                .build();

        // When / Then
        assertDoesNotThrow(() -> validReview.onCreate());
        assertNotNull(validReview.getCreatedAt());
    }

    @Test
    @DisplayName("isEventReview and isOutingReview - Should be mutually exclusive")
    void isEventReviewAndIsOutingReview_MutuallyExclusive() {
        // Test event review
        review.setEvent(event);
        review.setOuting(null);
        assertTrue(review.isEventReview());
        assertFalse(review.isOutingReview());

        // Test outing review
        review.setEvent(null);
        review.setOuting(outing);
        assertFalse(review.isEventReview());
        assertTrue(review.isOutingReview());
    }

    @Test
    @DisplayName("Builder - Should build review with all fields")
    void builder_AllFields_BuildsCorrectly() {
        // Given / When
        Review builtReview = Review.builder()
                .id(UUID.randomUUID())
                .reviewer(reviewer)
                .event(event)
                .rating(4)
                .comment("Nice event")
                .createdAt(LocalDateTime.now())
                .build();

        // Then
        assertNotNull(builtReview);
        assertNotNull(builtReview.getId());
        assertEquals(reviewer, builtReview.getReviewer());
        assertEquals(event, builtReview.getEvent());
        assertEquals(4, builtReview.getRating());
        assertEquals("Nice event", builtReview.getComment());
        assertNotNull(builtReview.getCreatedAt());
    }

    @Test
    @DisplayName("Setters and Getters - Should work correctly")
    void settersAndGetters_WorkCorrectly() {
        // Given
        Review newReview = new Review();
        UUID newId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        newReview.setId(newId);
        newReview.setReviewer(reviewer);
        newReview.setEvent(event);
        newReview.setRating(3);
        newReview.setComment("Average");
        newReview.setCreatedAt(timestamp);

        // Then
        assertEquals(newId, newReview.getId());
        assertEquals(reviewer, newReview.getReviewer());
        assertEquals(event, newReview.getEvent());
        assertEquals(3, newReview.getRating());
        assertEquals("Average", newReview.getComment());
        assertEquals(timestamp, newReview.getCreatedAt());
    }

    @Test
    @DisplayName("onCreate - Should validate all ratings from 1 to 5")
    void onCreate_AllValidRatings_Valid() {
        // Test all valid ratings
        for (int rating = 1; rating <= 5; rating++) {
            Review validReview = Review.builder()
                    .reviewer(reviewer)
                    .event(event)
                    .rating(rating)
                    .comment("Rating " + rating)
                    .build();

            final int currentRating = rating;
            assertDoesNotThrow(() -> validReview.onCreate(),
                    "Rating " + currentRating + " should be valid");
        }
    }

    @Test
    @DisplayName("onCreate - Should validate multiple invalid ratings")
    void onCreate_MultipleInvalidRatings_ThrowsException() {
        // Test multiple invalid ratings
        int[] invalidRatings = {-5, -1, 0, 6, 10, 100};

        for (int rating : invalidRatings) {
            Review invalidReview = Review.builder()
                    .reviewer(reviewer)
                    .event(event)
                    .rating(rating)
                    .comment("Invalid rating " + rating)
                    .build();

            final int currentRating = rating;
            assertThrows(IllegalArgumentException.class,
                    () -> invalidReview.onCreate(),
                    "Rating " + currentRating + " should be invalid");
        }
    }

    @Test
    @DisplayName("Review with null comment - Should be valid")
    void review_NullComment_Valid() {
        // Given
        Review reviewWithoutComment = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(4)
                .comment(null)
                .build();

        // When / Then
        assertDoesNotThrow(() -> reviewWithoutComment.onCreate());
        assertNull(reviewWithoutComment.getComment());
    }

    @Test
    @DisplayName("Review with empty comment - Should be valid")
    void review_EmptyComment_Valid() {
        // Given
        Review reviewWithEmptyComment = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(4)
                .comment("")
                .build();

        // When / Then
        assertDoesNotThrow(() -> reviewWithEmptyComment.onCreate());
        assertEquals("", reviewWithEmptyComment.getComment());
    }

    @Test
    @DisplayName("Review with long comment - Should be valid")
    void review_LongComment_Valid() {
        // Given
        String longComment = "A".repeat(1000);
        Review reviewWithLongComment = Review.builder()
                .reviewer(reviewer)
                .event(event)
                .rating(5)
                .comment(longComment)
                .build();

        // When / Then
        assertDoesNotThrow(() -> reviewWithLongComment.onCreate());
        assertEquals(longComment, reviewWithLongComment.getComment());
    }
}
