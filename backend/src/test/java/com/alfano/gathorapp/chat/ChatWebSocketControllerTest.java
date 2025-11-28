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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatWebSocketController.
 * Uses Mockito to test WebSocket message handling logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController Tests")
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private UUID userId;
    private UUID outingId;
    private UUID chatId;
    private UUID messageId;
    private SecurityUser securityUser;
    private Principal principal;
    private Chat chat;
    private Outing outing;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        chatId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        // Create Principal as UsernamePasswordAuthenticationToken
        principal = new UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.getAuthorities()
        );

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
    @DisplayName("sendMessage - Should send and broadcast message successfully")
    void sendMessage_ValidRequest_SendsAndBroadcastsMessage() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello WebSocket!")
                .build();

        ChatMessageResponse messageResponse = ChatMessageResponse.builder()
                .id(messageId)
                .chatId(chatId)
                .content("Hello WebSocket!")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();

        when(chatService.sendMessage(outingId, request, userId)).thenReturn(messageResponse);

        // When
        chatWebSocketController.sendMessage(outingId, request, principal);

        // Then
        verify(chatService, times(1)).sendMessage(outingId, request, userId);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChatMessageResponse> messageCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);

        verify(messagingTemplate, times(1)).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals("/topic/chat/" + chatId, destinationCaptor.getValue());
        assertEquals(messageResponse, messageCaptor.getValue());
        assertEquals(messageId, messageCaptor.getValue().getId());
        assertEquals("Hello WebSocket!", messageCaptor.getValue().getContent());
    }

    @Test
    @DisplayName("sendMessage - Should handle service exception and send error to user")
    void sendMessage_ServiceThrowsException_SendsErrorToUser() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test message")
                .build();

        RuntimeException exception = new RuntimeException("Chat not found");
        when(chatService.sendMessage(outingId, request, userId)).thenThrow(exception);

        // When
        chatWebSocketController.sendMessage(outingId, request, principal);

        // Then
        verify(chatService, times(1)).sendMessage(outingId, request, userId);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> errorCaptor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                userCaptor.capture(),
                queueCaptor.capture(),
                errorCaptor.capture()
        );

        assertEquals(userId.toString(), userCaptor.getValue());
        assertEquals("/queue/errors", queueCaptor.getValue());

        // Verify error message content
        Object errorMessage = errorCaptor.getValue();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.toString().contains("Failed to send message"));

        // Should NOT broadcast to topic when error occurs
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("sendMessage - Should broadcast to correct chat topic")
    void sendMessage_DifferentChats_BroadcastsToCorrectTopic() {
        // Given
        UUID chatId1 = UUID.randomUUID();
        UUID chatId2 = UUID.randomUUID();

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .build();

        ChatMessageResponse messageResponse1 = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId1)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();

        ChatMessageResponse messageResponse2 = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId2)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();

        when(chatService.sendMessage(any(UUID.class), eq(request), eq(userId)))
                .thenReturn(messageResponse1)
                .thenReturn(messageResponse2);

        // When
        chatWebSocketController.sendMessage(UUID.randomUUID(), request, principal);
        chatWebSocketController.sendMessage(UUID.randomUUID(), request, principal);

        // Then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(2)).convertAndSend(
                destinationCaptor.capture(),
                any(ChatMessageResponse.class)
        );

        assertEquals("/topic/chat/" + chatId1, destinationCaptor.getAllValues().get(0));
        assertEquals("/topic/chat/" + chatId2, destinationCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("userTyping - Should broadcast typing indicator successfully")
    void userTyping_ValidRequest_BroadcastsTypingIndicator() {
        // Given
        when(chatService.getOrCreateChat(outingId)).thenReturn(chat);

        // When
        chatWebSocketController.userTyping(outingId, principal);

        // Then
        verify(chatService, times(1)).getOrCreateChat(outingId);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> indicatorCaptor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, times(1)).convertAndSend(
                destinationCaptor.capture(),
                indicatorCaptor.capture()
        );

        assertEquals("/topic/chat/" + chatId + "/typing", destinationCaptor.getValue());

        // Verify typing indicator contains correct user info
        Object indicator = indicatorCaptor.getValue();
        assertNotNull(indicator);
        assertTrue(indicator.toString().contains(userId.toString()));
        assertTrue(indicator.toString().contains("test@example.com")); // getUsername() returns email
    }

    @Test
    @DisplayName("userTyping - Should handle service exception gracefully")
    void userTyping_ServiceThrowsException_HandlesGracefully() {
        // Given
        RuntimeException exception = new RuntimeException("Chat not found");
        when(chatService.getOrCreateChat(outingId)).thenThrow(exception);

        // When / Then (should not throw, just log error)
        assertDoesNotThrow(() -> chatWebSocketController.userTyping(outingId, principal));

        verify(chatService, times(1)).getOrCreateChat(outingId);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("userTyping - Should broadcast to correct chat typing topic")
    void userTyping_DifferentChats_BroadcastsToCorrectTypingTopic() {
        // Given
        UUID outingId1 = UUID.randomUUID();
        UUID outingId2 = UUID.randomUUID();
        UUID chatId1 = UUID.randomUUID();
        UUID chatId2 = UUID.randomUUID();

        Chat chat1 = Chat.builder()
                .id(chatId1)
                .outing(outing)
                .active(true)
                .build();

        Chat chat2 = Chat.builder()
                .id(chatId2)
                .outing(outing)
                .active(true)
                .build();

        when(chatService.getOrCreateChat(outingId1)).thenReturn(chat1);
        when(chatService.getOrCreateChat(outingId2)).thenReturn(chat2);

        // When
        chatWebSocketController.userTyping(outingId1, principal);
        chatWebSocketController.userTyping(outingId2, principal);

        // Then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(2)).convertAndSend(
                destinationCaptor.capture(),
                any(Object.class)
        );

        assertEquals("/topic/chat/" + chatId1 + "/typing", destinationCaptor.getAllValues().get(0));
        assertEquals("/topic/chat/" + chatId2 + "/typing", destinationCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("sendMessage - Should handle different users sending messages")
    void sendMessage_DifferentUsers_ProcessesCorrectly() {
        // Given
        UUID user2Id = UUID.randomUUID();
        User user2 = User.builder()
                .id(user2Id)
                .name("User 2")
                .email("user2@example.com")
                .passwordHash("pass")
                .role(Role.USER)
                .build();

        SecurityUser securityUser2 = new SecurityUser(user2);
        Principal principal2 = new UsernamePasswordAuthenticationToken(
                securityUser2,
                null,
                securityUser2.getAuthorities()
        );

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .build();

        ChatMessageResponse response1 = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
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

        when(chatService.sendMessage(eq(outingId), eq(request), eq(userId)))
                .thenReturn(response1);
        when(chatService.sendMessage(eq(outingId), eq(request), eq(user2Id)))
                .thenReturn(response2);

        // When
        chatWebSocketController.sendMessage(outingId, request, principal);
        chatWebSocketController.sendMessage(outingId, request, principal2);

        // Then
        verify(chatService, times(1)).sendMessage(outingId, request, userId);
        verify(chatService, times(1)).sendMessage(outingId, request, user2Id);
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("sendMessage - Should handle invalid principal gracefully")
    void sendMessage_InvalidPrincipal_ThrowsException() {
        // Given
        Principal invalidPrincipal = () -> "invalidUser";

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .build();

        // When / Then
        assertThrows(RuntimeException.class, () -> {
            chatWebSocketController.sendMessage(outingId, request, invalidPrincipal);
        });

        verify(chatService, never()).sendMessage(any(), any(), any());
    }

    @Test
    @DisplayName("userTyping - Should handle invalid principal gracefully")
    void userTyping_InvalidPrincipal_ThrowsException() {
        // Given
        Principal invalidPrincipal = () -> "invalidUser";

        // When / Then
        assertThrows(RuntimeException.class, () -> {
            chatWebSocketController.userTyping(outingId, invalidPrincipal);
        });

        verify(chatService, never()).getOrCreateChat(any());
    }

    @Test
    @DisplayName("sendMessage - Should handle empty message content")
    void sendMessage_EmptyContent_ProcessesNormally() {
        // Given
        SendMessageRequest request = SendMessageRequest.builder()
                .content("")
                .build();

        ChatMessageResponse messageResponse = ChatMessageResponse.builder()
                .id(messageId)
                .chatId(chatId)
                .content("")
                .timestamp(LocalDateTime.now())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .build())
                .build();

        when(chatService.sendMessage(outingId, request, userId)).thenReturn(messageResponse);

        // When
        chatWebSocketController.sendMessage(outingId, request, principal);

        // Then
        verify(chatService, times(1)).sendMessage(outingId, request, userId);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/" + chatId),
                any(ChatMessageResponse.class)
        );
    }
}
