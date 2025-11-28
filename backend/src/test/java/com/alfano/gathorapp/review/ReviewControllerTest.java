package com.alfano.gathorapp.review;

import com.alfano.gathorapp.review.dto.CreateReviewRequest;
import com.alfano.gathorapp.review.dto.ReviewResponse;
import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController Tests")
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private UUID userId;
    private UUID eventId;
    private UUID outingId;
    private UUID reviewId;
    private SecurityUser securityUser;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        reviewResponse = ReviewResponse.builder()
                .id(reviewId)
                .rating(5)
                .comment("Great experience!")
                .createdAt(LocalDateTime.now())
                .reviewer(ReviewResponse.ReviewerInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();
    }

    @Test
    @DisplayName("GET /api/reviews/event/{eventId} - Should return reviews for event")
    void getReviewsByEvent_ReturnsEventReviews() {
        // Given
        List<ReviewResponse> mockReviews = new ArrayList<>();
        mockReviews.add(reviewResponse);
        when(reviewService.getReviewsByEvent(eventId)).thenReturn(mockReviews);

        // When
        ResponseEntity<List<ReviewResponse>> response =
                reviewController.getReviewsByEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(reviewId, response.getBody().get(0).getId());
        assertEquals(5, response.getBody().get(0).getRating());
        verify(reviewService, times(1)).getReviewsByEvent(eventId);
    }

    @Test
    @DisplayName("GET /api/reviews/event/{eventId} - Should return empty list when no reviews")
    void getReviewsByEvent_NoReviews_ReturnsEmptyList() {
        // Given
        when(reviewService.getReviewsByEvent(eventId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<ReviewResponse>> response =
                reviewController.getReviewsByEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(reviewService, times(1)).getReviewsByEvent(eventId);
    }

    @Test
    @DisplayName("GET /api/reviews/event/{eventId} - Should return multiple reviews")
    void getReviewsByEvent_MultipleReviews_ReturnsAll() {
        // Given
        List<ReviewResponse> mockReviews = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockReviews.add(ReviewResponse.builder()
                    .id(UUID.randomUUID())
                    .rating(i + 1)
                    .comment("Review " + i)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        when(reviewService.getReviewsByEvent(eventId)).thenReturn(mockReviews);

        // When
        ResponseEntity<List<ReviewResponse>> response =
                reviewController.getReviewsByEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().size());
        verify(reviewService, times(1)).getReviewsByEvent(eventId);
    }

    @Test
    @DisplayName("GET /api/reviews/outing/{outingId} - Should return reviews for outing")
    void getReviewsByOuting_ReturnsOutingReviews() {
        // Given
        List<ReviewResponse> mockReviews = new ArrayList<>();
        mockReviews.add(reviewResponse);
        when(reviewService.getReviewsByOuting(outingId)).thenReturn(mockReviews);

        // When
        ResponseEntity<List<ReviewResponse>> response =
                reviewController.getReviewsByOuting(outingId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(reviewId, response.getBody().get(0).getId());
        verify(reviewService, times(1)).getReviewsByOuting(outingId);
    }

    @Test
    @DisplayName("GET /api/reviews/outing/{outingId} - Should return empty list when no reviews")
    void getReviewsByOuting_NoReviews_ReturnsEmptyList() {
        // Given
        when(reviewService.getReviewsByOuting(outingId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<ReviewResponse>> response =
                reviewController.getReviewsByOuting(outingId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(reviewService, times(1)).getReviewsByOuting(outingId);
    }

    @Test
    @DisplayName("POST /api/reviews - Should create review successfully")
    void createReview_ValidRequest_CreatesReview() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .comment("Excellent!")
                .build();

        when(reviewService.createReview(request, userId)).thenReturn(reviewResponse);

        // When
        ResponseEntity<ReviewResponse> response =
                reviewController.createReview(request, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(reviewId, response.getBody().getId());
        assertEquals(5, response.getBody().getRating());
        assertEquals("Great experience!", response.getBody().getComment());
        verify(reviewService, times(1)).createReview(request, userId);
    }

    @Test
    @DisplayName("POST /api/reviews - Should create review for outing")
    void createReview_ForOuting_CreatesReview() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .outingId(outingId)
                .rating(4)
                .comment("Good outing")
                .build();

        ReviewResponse outingReview = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .rating(4)
                .comment("Good outing")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(request, userId)).thenReturn(outingReview);

        // When
        ResponseEntity<ReviewResponse> response =
                reviewController.createReview(request, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(4, response.getBody().getRating());
        assertEquals("Good outing", response.getBody().getComment());
        verify(reviewService, times(1)).createReview(request, userId);
    }

    @Test
    @DisplayName("POST /api/reviews - Should create reviews with different ratings")
    void createReview_DifferentRatings_CreatesEachCorrectly() {
        // Given
        CreateReviewRequest request1 = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .comment("Perfect!")
                .build();

        CreateReviewRequest request2 = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(3)
                .comment("Average")
                .build();

        ReviewResponse review1 = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .rating(5)
                .comment("Perfect!")
                .createdAt(LocalDateTime.now())
                .build();

        ReviewResponse review2 = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .rating(3)
                .comment("Average")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(request1, userId)).thenReturn(review1);
        when(reviewService.createReview(request2, userId)).thenReturn(review2);

        // When
        ResponseEntity<ReviewResponse> response1 =
                reviewController.createReview(request1, securityUser);
        ResponseEntity<ReviewResponse> response2 =
                reviewController.createReview(request2, securityUser);

        // Then
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertEquals(5, response1.getBody().getRating());
        assertEquals(3, response2.getBody().getRating());
        verify(reviewService, times(1)).createReview(request1, userId);
        verify(reviewService, times(1)).createReview(request2, userId);
    }

    @Test
    @DisplayName("GET /api/reviews/event/{eventId} - Should handle different events")
    void getReviewsByEvent_DifferentEvents_ReturnsCorrectReviews() {
        // Given
        UUID eventId2 = UUID.randomUUID();
        List<ReviewResponse> reviews1 = List.of(reviewResponse);
        List<ReviewResponse> reviews2 = List.of(ReviewResponse.builder()
                .id(UUID.randomUUID())
                .rating(4)
                .comment("Good")
                .build());

        when(reviewService.getReviewsByEvent(eventId)).thenReturn(reviews1);
        when(reviewService.getReviewsByEvent(eventId2)).thenReturn(reviews2);

        // When
        ResponseEntity<List<ReviewResponse>> response1 =
                reviewController.getReviewsByEvent(eventId);
        ResponseEntity<List<ReviewResponse>> response2 =
                reviewController.getReviewsByEvent(eventId2);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(5, response1.getBody().get(0).getRating());
        assertEquals(4, response2.getBody().get(0).getRating());
        verify(reviewService, times(1)).getReviewsByEvent(eventId);
        verify(reviewService, times(1)).getReviewsByEvent(eventId2);
    }

    @Test
    @DisplayName("GET /api/reviews/outing/{outingId} - Should handle different outings")
    void getReviewsByOuting_DifferentOutings_ReturnsCorrectReviews() {
        // Given
        UUID outingId2 = UUID.randomUUID();
        List<ReviewResponse> reviews1 = List.of(reviewResponse);
        List<ReviewResponse> reviews2 = List.of(ReviewResponse.builder()
                .id(UUID.randomUUID())
                .rating(3)
                .comment("OK")
                .build());

        when(reviewService.getReviewsByOuting(outingId)).thenReturn(reviews1);
        when(reviewService.getReviewsByOuting(outingId2)).thenReturn(reviews2);

        // When
        ResponseEntity<List<ReviewResponse>> response1 =
                reviewController.getReviewsByOuting(outingId);
        ResponseEntity<List<ReviewResponse>> response2 =
                reviewController.getReviewsByOuting(outingId2);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(5, response1.getBody().get(0).getRating());
        assertEquals(3, response2.getBody().get(0).getRating());
        verify(reviewService, times(1)).getReviewsByOuting(outingId);
        verify(reviewService, times(1)).getReviewsByOuting(outingId2);
    }
}
