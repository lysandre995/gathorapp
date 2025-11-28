package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.review.dto.CreateReviewRequest;
import com.alfano.gathorapp.review.dto.ReviewResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for ReviewService.
 * Tests CRUD operations, validations, and business rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Event testEvent;
    private Outing testOuting;
    private Review testReview;
    private UUID userId;
    private UUID eventId;
    private UUID outingId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();

        User organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Organizer")
                .email("organizer@example.com")
                .role(Role.PREMIUM)
                .build();

        testOuting = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Description")
                .organizer(organizer)
                .maxParticipants(5)
                .outingDate(LocalDateTime.now().plusDays(1))
                .build();

        testReview = Review.builder()
                .id(reviewId)
                .reviewer(testUser)
                .event(testEvent)
                .rating(5)
                .comment("Great event!")
                .build();
    }

    // ==================== getReviewsByEvent Tests ====================

    @Test
    @DisplayName("Should get all reviews for an event")
    void getReviewsByEvent_Success() {
        when(reviewRepository.findByEventId(eventId)).thenReturn(List.of(testReview));
        when(reviewMapper.toResponse(testReview))
                .thenReturn(ReviewResponse.builder().id(reviewId).rating(5).build());

        List<ReviewResponse> result = reviewService.getReviewsByEvent(eventId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(reviewId);
        assertThat(result.get(0).getRating()).isEqualTo(5);
        verify(reviewRepository).findByEventId(eventId);
    }

    @Test
    @DisplayName("Should return empty list when no reviews for event")
    void getReviewsByEvent_EmptyList() {
        when(reviewRepository.findByEventId(eventId)).thenReturn(List.of());

        List<ReviewResponse> result = reviewService.getReviewsByEvent(eventId);

        assertThat(result).isEmpty();
        verify(reviewRepository).findByEventId(eventId);
    }

    // ==================== getReviewsByOuting Tests ====================

    @Test
    @DisplayName("Should get all reviews for an outing")
    void getReviewsByOuting_Success() {
        Review outingReview = Review.builder()
                .id(reviewId)
                .reviewer(testUser)
                .outing(testOuting)
                .rating(4)
                .comment("Nice outing!")
                .build();

        when(reviewRepository.findByOutingId(outingId)).thenReturn(List.of(outingReview));
        when(reviewMapper.toResponse(outingReview))
                .thenReturn(ReviewResponse.builder().id(reviewId).rating(4).build());

        List<ReviewResponse> result = reviewService.getReviewsByOuting(outingId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(reviewId);
        assertThat(result.get(0).getRating()).isEqualTo(4);
        verify(reviewRepository).findByOutingId(outingId);
    }

    // ==================== createReview for Event Tests ====================

    @Test
    @DisplayName("Should create review for event successfully")
    void createReview_ForEvent_Success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .comment("Great event!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(reviewRepository.existsByReviewerAndEvent(testUser, testEvent)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toResponse(testReview))
                .thenReturn(ReviewResponse.builder().id(reviewId).rating(5).build());

        ReviewResponse result = reviewService.createReview(request, userId);

        assertThat(result.getId()).isEqualTo(reviewId);
        assertThat(result.getRating()).isEqualTo(5);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void createReview_UserNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when both event and outing provided")
    void createReview_BothEventAndOuting() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .outingId(outingId)
                .rating(5)
                .build();

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be for either an event or an outing, not both");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when neither event nor outing provided")
    void createReview_NeitherEventNorOuting() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .build();

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be for either an event or an outing");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when event not found")
    void createReview_EventNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already reviewed event")
    void createReview_EventAlreadyReviewed() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(reviewRepository.existsByReviewerAndEvent(testUser, testEvent)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reviewed this event");

        verify(reviewRepository, never()).save(any());
    }

    // ==================== createReview for Outing Tests ====================

    @Test
    @DisplayName("Should create review for outing successfully")
    void createReview_ForOuting_Success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .outingId(outingId)
                .rating(4)
                .comment("Nice outing!")
                .build();

        Review outingReview = Review.builder()
                .id(reviewId)
                .reviewer(testUser)
                .outing(testOuting)
                .rating(4)
                .comment("Nice outing!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(reviewRepository.existsByReviewerAndOuting(testUser, testOuting)).thenReturn(false);
        when(participationRepository.existsByUserAndOuting(testUser, testOuting)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(outingReview);
        when(reviewMapper.toResponse(outingReview))
                .thenReturn(ReviewResponse.builder().id(reviewId).rating(4).build());

        ReviewResponse result = reviewService.createReview(request, userId);

        assertThat(result.getId()).isEqualTo(reviewId);
        assertThat(result.getRating()).isEqualTo(4);
        verify(reviewRepository).save(any(Review.class));
        verify(participationRepository).existsByUserAndOuting(testUser, testOuting);
    }

    @Test
    @DisplayName("Should throw exception when outing not found")
    void createReview_OutingNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .outingId(outingId)
                .rating(4)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outing not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already reviewed outing")
    void createReview_OutingAlreadyReviewed() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .outingId(outingId)
                .rating(4)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(reviewRepository.existsByReviewerAndOuting(testUser, testOuting)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reviewed this outing");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user did not participate in outing")
    void createReview_OutingNotParticipated() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .outingId(outingId)
                .rating(4)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(testOuting));
        when(reviewRepository.existsByReviewerAndOuting(testUser, testOuting)).thenReturn(false);
        when(participationRepository.existsByUserAndOuting(testUser, testOuting)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must participate in the outing to review it");

        verify(reviewRepository, never()).save(any());
    }

    // ==================== Rating Validation Tests ====================

    @Test
    @DisplayName("Should accept valid rating of 1")
    void createReview_Rating1_Valid() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(1)
                .comment("Poor")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(reviewRepository.existsByReviewerAndEvent(testUser, testEvent)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toResponse(any())).thenReturn(ReviewResponse.builder().rating(1).build());

        reviewService.createReview(request, userId);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Should accept valid rating of 5")
    void createReview_Rating5_Valid() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .eventId(eventId)
                .rating(5)
                .comment("Excellent")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(reviewRepository.existsByReviewerAndEvent(testUser, testEvent)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toResponse(any())).thenReturn(ReviewResponse.builder().rating(5).build());

        reviewService.createReview(request, userId);

        verify(reviewRepository).save(any(Review.class));
    }
}
