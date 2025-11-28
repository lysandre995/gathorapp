package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.outing.Outing;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatController.
 * Uses Mockito to test controller logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Tests")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private UUID userId;
    private UUID outingId;
    private UUID chatId;
    private SecurityUser securityUser;
    private Chat chat;
    private Outing outing;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        chatId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .build();

        chat = Chat.builder()
                .id(chatId)
                .outing(outing)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/chats/outing/{outingId} - Should return chat info")
    void getChatInfo_ReturnsOutingChatInfo() {
        // Given
        when(chatService.getOrCreateChat(outingId)).thenReturn(chat);

        // When
        ResponseEntity<ChatController.ChatInfoResponse> response =
                chatController.getChatInfo(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(chatId, response.getBody().chatId());
        assertEquals(outingId, response.getBody().outingId());
        verify(chatService, times(1)).getOrCreateChat(outingId);
    }

    @Test
    @DisplayName("GET /api/chats/outing/{outingId}/messages - Should return all messages")
    void getMessages_ReturnsMessageList() {
        // Given
        List<ChatMessageResponse> messages = new ArrayList<>();
        ChatMessageResponse message = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .content("Test message")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();
        messages.add(message);

        when(chatService.getMessages(outingId, userId)).thenReturn(messages);

        // When
        ResponseEntity<List<ChatMessageResponse>> response =
                chatController.getMessages(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Test message", response.getBody().get(0).getContent());
        verify(chatService, times(1)).getMessages(outingId, userId);
    }

    @Test
    @DisplayName("GET /api/chats/outing/{outingId}/messages - Should return empty list when no messages")
    void getMessages_EmptyChat_ReturnsEmptyList() {
        // Given
        when(chatService.getMessages(outingId, userId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<ChatMessageResponse>> response =
                chatController.getMessages(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(chatService, times(1)).getMessages(outingId, userId);
    }

    @Test
    @DisplayName("POST /api/chats/outing/{outingId}/messages - Should send message successfully")
    void sendMessage_Success_ReturnsCreatedMessage() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("New message")
                .build();

        ChatMessageResponse messageResponse = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .content("New message")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();

        when(chatService.sendMessage(outingId, request, userId)).thenReturn(messageResponse);

        // When
        ResponseEntity<ChatMessageResponse> response =
                chatController.sendMessage(outingId, request, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("New message", response.getBody().getContent());
        assertEquals(userId, response.getBody().getSender().getId());
        verify(chatService, times(1)).sendMessage(outingId, request, userId);
    }

    @Test
    @DisplayName("GET /api/chats/outing/{outingId} - Should handle different outings")
    void getChatInfo_DifferentOutings_ReturnsCorrectChat() {
        // Given
        UUID outingId2 = UUID.randomUUID();
        UUID chatId2 = UUID.randomUUID();

        Outing outing2 = Outing.builder()
                .id(outingId2)
                .title("Another Outing")
                .build();

        Chat chat2 = Chat.builder()
                .id(chatId2)
                .outing(outing2)
                .active(true)
                .build();

        when(chatService.getOrCreateChat(outingId)).thenReturn(chat);
        when(chatService.getOrCreateChat(outingId2)).thenReturn(chat2);

        // When
        ResponseEntity<ChatController.ChatInfoResponse> response1 =
                chatController.getChatInfo(outingId, securityUser);
        ResponseEntity<ChatController.ChatInfoResponse> response2 =
                chatController.getChatInfo(outingId2, securityUser);

        // Then
        assertEquals(chatId, response1.getBody().chatId());
        assertEquals(outingId, response1.getBody().outingId());

        assertEquals(chatId2, response2.getBody().chatId());
        assertEquals(outingId2, response2.getBody().outingId());
    }

    @Test
    @DisplayName("GET /api/chats/outing/{outingId}/messages - Should handle multiple messages")
    void getMessages_MultipleMessages_ReturnsAllMessages() {
        // Given
        List<ChatMessageResponse> messages = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            messages.add(ChatMessageResponse.builder()
                    .id(UUID.randomUUID())
                    .chatId(chatId)
                    .content("Message " + i)
                    .timestamp(LocalDateTime.now().minusMinutes(i))
                    .sender(ChatMessageResponse.SenderInfo.builder()
                            .id(userId)
                            .name("Test User")
                            .build())
                    .build());
        }

        when(chatService.getMessages(outingId, userId)).thenReturn(messages);

        // When
        ResponseEntity<List<ChatMessageResponse>> response =
                chatController.getMessages(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().size());
        verify(chatService, times(1)).getMessages(outingId, userId);
    }

    @Test
    @DisplayName("POST /api/chats/outing/{outingId}/messages - Should handle different users sending messages")
    void sendMessage_DifferentUsers_CallsServiceWithCorrectUserId() {
        // Given
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        User user1 = User.builder()
                .id(user1Id)
                .name("User 1")
                .email("user1@example.com")
                .passwordHash("pass1")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .id(user2Id)
                .name("User 2")
                .email("user2@example.com")
                .passwordHash("pass2")
                .role(Role.USER)
                .build();

        SecurityUser securityUser1 = new SecurityUser(user1);
        SecurityUser securityUser2 = new SecurityUser(user2);

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .build();

        ChatMessageResponse response1 = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(user1Id)
                        .name("User 1")
                        .build())
                .build();

        ChatMessageResponse response2 = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(user2Id)
                        .name("User 2")
                        .build())
                .build();

        when(chatService.sendMessage(eq(outingId), any(SendMessageRequest.class), eq(user1Id)))
                .thenReturn(response1);
        when(chatService.sendMessage(eq(outingId), any(SendMessageRequest.class), eq(user2Id)))
                .thenReturn(response2);

        // When
        chatController.sendMessage(outingId, request, securityUser1);
        chatController.sendMessage(outingId, request, securityUser2);

        // Then
        verify(chatService, times(1)).sendMessage(eq(outingId), any(SendMessageRequest.class), eq(user1Id));
        verify(chatService, times(1)).sendMessage(eq(outingId), any(SendMessageRequest.class), eq(user2Id));
    }
}
