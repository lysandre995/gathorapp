package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ParticipationController.
 * Tests the full participation workflow including concurrency control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ParticipationController Integration Tests")
class ParticipationControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OutingRepository outingRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User organizer;
        private User participant;
        private Outing testOuting;
        private String organizerToken;
        private String participantToken;

        @BeforeEach
        void setUp() {
                // Ensure a clean database state for each test
                testDatabaseCleaner.truncateAll();

                // Create organizer
                organizer = User.builder()
                                .name("Organizer")
                                .email("organizer@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                organizer = userRepository.save(organizer);

                // Create participant
                participant = User.builder()
                                .name("Participant")
                                .email("participant@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                participant = userRepository.save(participant);

                // Create test outing
                testOuting = Outing.builder()
                                .title("Test Outing")
                                .description("Test Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .organizer(organizer)
                                .build();
                testOuting = outingRepository.save(testOuting);

                // Generate tokens
                organizerToken = jwtTokenProvider.generateAccessToken(
                                organizer.getId(), organizer.getEmail(), organizer.getRole().name());
                participantToken = jwtTokenProvider.generateAccessToken(
                                participant.getId(), participant.getEmail(), participant.getRole().name());
        }

        @Test
        @DisplayName("POST /api/participations/outing/{outingId} - Should join outing")
        void joinOuting_Success() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", is("PENDING")))
                                .andExpect(jsonPath("$.user.id", is(participant.getId().toString())))
                                .andExpect(jsonPath("$.outing.id", is(testOuting.getId().toString())));
        }

        @Test
        @DisplayName("POST /api/participations/outing/{outingId} - Should fail if already joined")
        void joinOuting_AlreadyJoined() throws Exception {
                // Given - join once
                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated());

                // When & Then - try to join again
                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /api/participations/{id} - Should leave outing")
        void leaveOuting_Success() throws Exception {
                // Given - join first
                String response = mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String participationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                // When & Then - leave
                mockMvc.perform(delete("/api/participations/" + participationId)
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isNoContent());

                // Verify can join again
                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /api/participations/{id}/approve - Organizer should approve participation")
        void approveParticipation_Success() throws Exception {
                // Given - participant joins
                String response = mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String participationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                // When & Then - organizer approves
                mockMvc.perform(put("/api/participations/" + participationId + "/approve")
                                .header("Authorization", "Bearer " + organizerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("APPROVED")));
        }

        @Test
        @DisplayName("PUT /api/participations/{id}/reject - Organizer should reject participation")
        void rejectParticipation_Success() throws Exception {
                // Given - participant joins
                String response = mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String participationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                // When & Then - organizer rejects
                mockMvc.perform(put("/api/participations/" + participationId + "/reject")
                                .header("Authorization", "Bearer " + organizerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("REJECTED")));
        }

        @Test
        @DisplayName("PUT /api/participations/{id}/approve - Non-organizer should fail")
        void approveParticipation_Unauthorized() throws Exception {
                // Given - participant joins
                String response = mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String participationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                // Create another user (not organizer)
                User otherUser = User.builder()
                                .name("Other User")
                                .email("other@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                otherUser = userRepository.save(otherUser);
                String otherToken = jwtTokenProvider.generateAccessToken(
                                otherUser.getId(), otherUser.getEmail(), otherUser.getRole().name());

                // When & Then - other user tries to approve
                mockMvc.perform(put("/api/participations/" + participationId + "/approve")
                                .header("Authorization", "Bearer " + otherToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/participations/outing/{outingId} - Should return all participations for outing")
        void getParticipations_Success() throws Exception {
                // Given - create multiple participations
                User user2 = User.builder()
                                .name("User 2")
                                .email("user2@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                user2 = userRepository.save(user2);
                String user2Token = jwtTokenProvider.generateAccessToken(
                                user2.getId(), user2.getEmail(), user2.getRole().name());

                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + user2Token))
                                .andExpect(status().isCreated());

                // When & Then
                mockMvc.perform(get("/api/participations/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + organizerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
        }

        @Test
        @DisplayName("POST /api/participations/outing/{outingId} - Should fail when outing is full")
        void joinOuting_Full() throws Exception {
                // Given - create outing with only 1 spot
                Outing smallOuting = Outing.builder()
                                .title("Small Outing")
                                .description("Only 1 spot")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(1)
                                .organizer(organizer)
                                .build();
                smallOuting = outingRepository.save(smallOuting);

                // Join and approve first participant
                String response = mockMvc.perform(post("/api/participations/outing/" + smallOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String participationId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                mockMvc.perform(put("/api/participations/" + participationId + "/approve")
                                .header("Authorization", "Bearer " + organizerToken))
                                .andExpect(status().isOk());

                // Create another user
                User user2 = User.builder()
                                .name("User 2")
                                .email("user2@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                user2 = userRepository.save(user2);
                String user2Token = jwtTokenProvider.generateAccessToken(
                                user2.getId(), user2.getEmail(), user2.getRole().name());

                // When & Then - second user tries to join (outing is full)
                mockMvc.perform(post("/api/participations/outing/" + smallOuting.getId())
                                .header("Authorization", "Bearer " + user2Token))
                                .andExpect(status().isBadRequest());
        }
}
