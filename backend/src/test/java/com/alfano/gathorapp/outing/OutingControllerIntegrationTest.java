package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OutingController.
 * Tests the full stack: Controller → Service → Repository → Database
 * Uses real Spring context and in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("OutingController Integration Tests")
class OutingControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OutingRepository outingRepository;

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User testUser;
        private User premiumUser;
        private Event testEvent;
        private String userToken;
        private String premiumToken;

        @BeforeEach
        void setUp() {
                // Ensure a clean database state for each test
                testDatabaseCleaner.truncateAll();

                // Create test users
                testUser = User.builder()
                                .name("Test User")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                testUser = userRepository.save(testUser);

                premiumUser = User.builder()
                                .name("Premium User")
                                .email("premium@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.PREMIUM)
                                .build();
                premiumUser = userRepository.save(premiumUser);

                // Create test event
                testEvent = Event.builder()
                                .title("Test Event")
                                .description("Test Event Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .eventDate(LocalDateTime.now().plusDays(30))
                                .creator(premiumUser)
                                .build();
                testEvent = eventRepository.save(testEvent);

                // Generate JWT tokens
                userToken = jwtTokenProvider.generateAccessToken(testUser.getId(), testUser.getEmail(),
                                testUser.getRole().name());
                premiumToken = jwtTokenProvider.generateAccessToken(premiumUser.getId(), premiumUser.getEmail(),
                                premiumUser.getRole().name());
        }

        @Test
        @DisplayName("GET /api/outings - Should return all outings")
        void getAllOutings_Success() throws Exception {
                // Given - create test outing
                Outing outing = Outing.builder()
                                .title("Test Outing")
                                .description("Test Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .organizer(testUser)
                                .build();
                outingRepository.save(outing);

                // When & Then
                mockMvc.perform(get("/api/outings")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Test Outing")))
                                .andExpect(jsonPath("$[0].location", is("Test Location")))
                                .andExpect(jsonPath("$[0].maxParticipants", is(10)));
        }

        @Test
        @DisplayName("POST /api/outings - Should create independent outing")
        void createOuting_Independent_Success() throws Exception {
                // Given
                CreateOutingRequest request = CreateOutingRequest.builder()
                                .title("New Outing")
                                .description("New Description")
                                .location("New Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .build();

                // When & Then
                mockMvc.perform(post("/api/outings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title", is("New Outing")))
                                .andExpect(jsonPath("$.description", is("New Description")))
                                .andExpect(jsonPath("$.maxParticipants", is(10)))
                                .andExpect(jsonPath("$.currentParticipants", is(0)));
        }

        @Test
        @DisplayName("POST /api/outings - Premium user should create event-linked outing")
        void createOuting_EventLinked_Success() throws Exception {
                // Given
                CreateOutingRequest request = CreateOutingRequest.builder()
                                .title("Event Outing")
                                .description("Event Linked Outing")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(20)
                                .eventId(testEvent.getId())
                                .build();

                // When & Then
                mockMvc.perform(post("/api/outings")
                                .header("Authorization", "Bearer " + premiumToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title", is("Event Outing")))
                                .andExpect(jsonPath("$.event.id", is(testEvent.getId().toString())));
        }

        @Test
        @DisplayName("GET /api/outings/{id} - Should return outing by id")
        void getOutingById_Success() throws Exception {
                // Given
                Outing outing = Outing.builder()
                                .title("Test Outing")
                                .description("Test Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .organizer(testUser)
                                .build();
                outing = outingRepository.save(outing);

                // When & Then
                mockMvc.perform(get("/api/outings/" + outing.getId())
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(outing.getId().toString())))
                                .andExpect(jsonPath("$.title", is("Test Outing")));
        }

        @Test
        @DisplayName("GET /api/outings/{id} - Should return 404 for non-existent outing")
        void getOutingById_NotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();

                // When & Then
                mockMvc.perform(get("/api/outings/" + nonExistentId)
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/outings/upcoming - Should return only upcoming outings")
        void getUpcomingOutings_Success() throws Exception {
                // Given - create past and future outings
                Outing pastOuting = Outing.builder()
                                .title("Past Outing")
                                .description("Already happened")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().minusDays(1))
                                .maxParticipants(10)
                                .organizer(testUser)
                                .build();
                outingRepository.save(pastOuting);

                Outing futureOuting = Outing.builder()
                                .title("Future Outing")
                                .description("Will happen")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .organizer(testUser)
                                .build();
                outingRepository.save(futureOuting);

                // When & Then
                mockMvc.perform(get("/api/outings/upcoming")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Future Outing")));
        }

        @Test
        @DisplayName("POST /api/outings - Should fail without authentication")
        void createOuting_Unauthorized() throws Exception {
                // Given
                CreateOutingRequest request = CreateOutingRequest.builder()
                                .title("New Outing")
                                .description("New Description")
                                .location("New Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .build();

                // When & Then
                mockMvc.perform(post("/api/outings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }
}
