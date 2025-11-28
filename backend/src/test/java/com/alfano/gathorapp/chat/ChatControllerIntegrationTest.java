package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.Participation;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.participation.ParticipationStatus;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ChatController.
 * Tests real-time chat functionality within outings.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ChatController Integration Tests")
class ChatControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OutingRepository outingRepository;

        @Autowired
        private ParticipationRepository participationRepository;

        @Autowired
        private ChatRepository chatRepository;

        @Autowired
        private ChatMessageRepository chatMessageRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User organizer;
        private User participant;
        private Outing testOuting;
        private Chat testChat;
        private String organizerToken;
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

                // Create outing
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

                // Create chat
                testChat = Chat.builder()
                                .outing(testOuting)
                                .active(true)
                                .build();
                testChat = chatRepository.save(testChat);

                // Create approved participation
                Participation participation = Participation.builder()
                                .outing(testOuting)
                                .user(participant)
                                .status(ParticipationStatus.APPROVED)
                                .build();
                participationRepository.save(participation);

                // Generate tokens
                organizerToken = jwtTokenProvider.generateAccessToken(
                                organizer.getId(), organizer.getEmail(), organizer.getRole().name());
                participantToken = jwtTokenProvider.generateAccessToken(
                                participant.getId(), participant.getEmail(), participant.getRole().name());
        }

        @Test
        @DisplayName("POST /api/chats/outing/{outingId}/messages - Should send message")
        void sendMessage_Success() throws Exception {
                // Given
                SendMessageRequest request = SendMessageRequest.builder()
                                .content("Hello from integration test!")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/chats/outing/" + testOuting.getId() + "/messages")
                                .header("Authorization", "Bearer " + participantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.content", is("Hello from integration test!")))
                                .andExpect(jsonPath("$.sender.id", is(participant.getId().toString())))
                                .andExpect(jsonPath("$.sender.name", is("Participant")));
        }

        @Test
        @DisplayName("GET /api/chats/outing/{outingId}/messages - Should retrieve messages")
        void getMessages_Success() throws Exception {
                // Given - send a message first
                ChatMessage message = ChatMessage.builder()
                                .chat(testChat)
                                .sender(participant)
                                .content("Test message")
                                .timestamp(LocalDateTime.now())
                                .build();
                chatMessageRepository.save(message);

                // When & Then
                mockMvc.perform(get("/api/chats/outing/" + testOuting.getId() + "/messages")
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].content", is("Test message")))
                                .andExpect(jsonPath("$[0].sender.name", is("Participant")));
        }

        @Test
        @DisplayName("POST /api/chats/outing/{outingId}/messages - Should fail for non-participant")
        void sendMessage_NotParticipant() throws Exception {
                // Given - create another user who is not a participant
                User nonParticipant = User.builder()
                                .name("Non Participant")
                                .email("nonparticipant@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                nonParticipant = userRepository.save(nonParticipant);
                String nonParticipantToken = jwtTokenProvider.generateAccessToken(
                                nonParticipant.getId(), nonParticipant.getEmail(), nonParticipant.getRole().name());

                SendMessageRequest request = SendMessageRequest.builder()
                                .content("I should not be able to send this!")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/chats/outing/" + testOuting.getId() + "/messages")
                                .header("Authorization", "Bearer " + nonParticipantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/chats/outing/{outingId}/messages - Organizer should be able to send messages")
        void sendMessage_Organizer_Success() throws Exception {
                // Given
                SendMessageRequest request = SendMessageRequest.builder()
                                .content("Message from organizer")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/chats/outing/" + testOuting.getId() + "/messages")
                                .header("Authorization", "Bearer " + organizerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.content", is("Message from organizer")))
                                .andExpect(jsonPath("$.sender.name", is("Organizer")));
        }

        @Test
        @DisplayName("GET /api/chats/outing/{outingId} - Should return chat info")
        void getChatInfo_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/chats/outing/" + testOuting.getId())
                                .header("Authorization", "Bearer " + participantToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.chatId", is(testChat.getId().toString())))
                                .andExpect(jsonPath("$.outingId", is(testOuting.getId().toString())));
        }

        @Test
        @DisplayName("POST /api/chats/outing/{outingId}/messages - Should fail if chat is inactive")
        void sendMessage_InactiveChat() throws Exception {
                // Given - deactivate chat
                testChat.setActive(false);
                chatRepository.save(testChat);

                SendMessageRequest request = SendMessageRequest.builder()
                                .content("Chat is closed!")
                                .build();

                // When & Then
                mockMvc.perform(post("/api/chats/outing/" + testOuting.getId() + "/messages")
                                .header("Authorization", "Bearer " + participantToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
