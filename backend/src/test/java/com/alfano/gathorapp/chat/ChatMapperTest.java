package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
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
 * Unit tests for ChatMapper.
 */
@DisplayName("ChatMapper Tests")
class ChatMapperTest {

    private ChatMapper chatMapper;
    private User sender;
    private Chat chat;

    @BeforeEach
    void setUp() {
        chatMapper = new ChatMapper();

        sender = User.builder()
                .id(UUID.randomUUID())
                .name("Test Sender")
                .email("sender@example.com")
                .role(Role.USER)
                .build();

        Outing outing = Outing.builder()
                .id(UUID.randomUUID())
                .title("Test Outing")
                .build();

        chat = Chat.builder()
                .id(UUID.randomUUID())
                .outing(outing)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should convert ChatMessage to ChatMessageResponse")
    void toMessageResponse_ConvertsCorrectly() {
        // Given
        UUID messageId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        ChatMessage message = ChatMessage.builder()
                .id(messageId)
                .chat(chat)
                .sender(sender)
                .content("Test message content")
                .timestamp(timestamp)
                .build();

        // When
        ChatMessageResponse response = chatMapper.toMessageResponse(message);

        // Then
        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals(chat.getId(), response.getChatId());
        assertEquals("Test message content", response.getContent());
        assertEquals(timestamp, response.getTimestamp());

        assertNotNull(response.getSender());
        assertEquals(sender.getId(), response.getSender().getId());
        assertEquals("Test Sender", response.getSender().getName());
    }

    @Test
    @DisplayName("Should handle empty message content")
    void toMessageResponse_EmptyContent_HandlesCorrectly() {
        // Given
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .sender(sender)
                .content("")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        ChatMessageResponse response = chatMapper.toMessageResponse(message);

        // Then
        assertNotNull(response);
        assertEquals("", response.getContent());
    }

    @Test
    @DisplayName("Should handle long message content")
    void toMessageResponse_LongContent_HandlesCorrectly() {
        // Given
        String longContent = "A".repeat(1000);
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .sender(sender)
                .content(longContent)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        ChatMessageResponse response = chatMapper.toMessageResponse(message);

        // Then
        assertNotNull(response);
        assertEquals(longContent, response.getContent());
    }

    @Test
    @DisplayName("Should map sender details correctly")
    void toMessageResponse_SenderDetails_MappedCorrectly() {
        // Given
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .sender(sender)
                .content("Test")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        ChatMessageResponse response = chatMapper.toMessageResponse(message);

        // Then
        assertNotNull(response.getSender());
        assertEquals(sender.getId(), response.getSender().getId());
        assertEquals(sender.getName(), response.getSender().getName());
    }
}
