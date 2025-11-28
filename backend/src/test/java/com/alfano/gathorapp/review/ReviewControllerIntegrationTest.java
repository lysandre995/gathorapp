package com.alfano.gathorapp.review;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.Participation;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.participation.ParticipationStatus;
import com.alfano.gathorapp.review.dto.CreateReviewRequest;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReviewController.
 * Tests review creation, retrieval, and rating functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ReviewController Integration Tests")
class ReviewControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OutingRepository outingRepository;

        @Autowired
        private ParticipationRepository participationRepository;

        @Autowired
        private ReviewRepository reviewRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User organizer;
        private User participant;
        private Outing testOuting;
        private String participantToken;

        @BeforeEach
        void setUp() {
                // Ensure a clean database state for each test
                testDatabaseCleaner.truncateAll();

                // Create users
                organizer = User.builder()
                                .name("Organizer")
                                .email("organizer@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                organizer = userRepository.save(organizer);

                participant = User.builder()
                                .name("Participant")
                                .email("participant@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                participant = userRepository.save(participant);

                // Create past outing (for reviews)
                testOuting = Outing.builder()
                                .title("Test Outing")
                                .description("Test Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().minusDays(1)) // Past date
                                .maxParticipants(10)
                                .organizer(organizer)
                                .build();
                testOuting = outingRepository.save(testOuting);

                // Create approved participation
                Participation participation = Participation.builder()
                                .outing(testOuting)
                                .user(participant)
                                .status(ParticipationStatus.APPROVED)
                                .build();
                participationRepository.save(participation);

                // Generate tokens
                participantToken = jwtTokenProvider.generateAccessToken(
                                participant.getId(), participant.getEmail(), participant.getRole().name());
        }

        @AfterEach
        void debugState() {
                long userCount = userRepository.count();
                long outingCount = outingRepository.count();
                long participationCount = participationRepository.count();
                long reviewCount = reviewRepository.count();

                System.out.println("=== AFTER TEST STATE ===");
                System.out.println("Users: " + userCount);
                System.out.println("Outings: " + outingCount);
                System.out.println("Participations: " + participationCount);
                System.out.println("Reviews: " + reviewCount);
        }

        @Test
        @DisplayName("POST /api/reviews/outings/{id} - Should create review")
        void createReview_Success() throws Exception {
                // Given
                CreateReviewRequest request = CreateReviewRequest.builder()
                                .rating(5)
                                .comment("Great outing!")
                                .build();

                System.out.println("=== TEST START: createReview_Success ===");
                System.out.println("Outing ID: " + testOuting.getId());
                System.out.println("Participant ID: " + participant.getId());
                System.out.println("Participant Token: " + participantToken);

                // When & Then
                mockMvc.perform(post("/api/reviews/outings/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.rating", is(5)))
                                .andExpect(jsonPath("$.comment", is("Great outing!")))
                                .andExpect(jsonPath("$.reviewer.id", is(participant.getId().toString())));
        }

        @Test
        @DisplayName("POST /api/reviews/outings/{id} - Should fail if user not a participant")
        void createReview_NotParticipant() throws Exception {
                // Given - create another user who didn't participate
                User nonParticipant = User.builder()
                                .name("Non Participant")
                                .email("nonparticipant@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                nonParticipant = userRepository.save(nonParticipant);
                String nonParticipantToken = jwtTokenProvider.generateAccessToken(
                                nonParticipant.getId(), nonParticipant.getEmail(), nonParticipant.getRole().name());

                CreateReviewRequest request = CreateReviewRequest.builder()
                                .rating(3)
                                .comment("I didn't even participate!")
                                .build();

                System.out.println("=== TEST START: createReview_Success ===");
                System.out.println("Outing ID: " + testOuting.getId());
                System.out.println("Participant ID: " + participant.getId());
                System.out.println("Participant Token: " + participantToken);

                // When & Then
                mockMvc.perform(post("/api/reviews/outings/" + testOuting.getId())
                                .header("Authorization", "Bearer " + nonParticipantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/reviews/outings/{id} - Should fail if already reviewed")
        void createReview_AlreadyReviewed() throws Exception {
                // Given - create initial review
                CreateReviewRequest request = CreateReviewRequest.builder()
                                .rating(4)
                                .comment("First review")
                                .build();

                mockMvc.perform(post("/api/reviews/outings/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // When & Then - try to review again
                CreateReviewRequest duplicateRequest = CreateReviewRequest.builder()
                                .rating(5)
                                .comment("Trying to review again")
                                .build();

                System.out.println("=== TEST START: createReview_Success ===");
                System.out.println("Outing ID: " + testOuting.getId());
                System.out.println("Participant ID: " + participant.getId());
                System.out.println("Participant Token: " + participantToken);

                mockMvc.perform(post("/api/reviews/outings/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateRequest)))
                                .andExpect(status().isBadRequest());
        }
}
