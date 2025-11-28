package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService using JUnit 5 and Mockito.
 *
 * Tests cover:
 * - Chat creation
 * - Message sending with permission checks
 * - Message retrieval
 * - Access control validation
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatService chatService;

    private UUID outingId;
    private UUID userId;
    private UUID organizerId;
    private User user;
    private User organizer;
    private Outing outing;
    private Chat chat;

    @BeforeEach
    void setUp() {
        outingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizerId = UUID.randomUUID();

        // Create test user
        user = new User();
        user.setId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");

        // Create test organizer
        organizer = new User();
        organizer.setId(organizerId);
        organizer.setName("Organizer");
        organizer.setEmail("organizer@example.com");

        // Create test outing
        outing = new Outing();
        outing.setId(outingId);
        outing.setTitle("Test Outing");
        outing.setOrganizer(organizer);

        // Create test chat
        chat = new Chat();
        chat.setId(UUID.randomUUID());
        chat.setOuting(outing);
        chat.setActive(true);
    }

    @Test
    void testGetOrCreateChat_WhenChatExists_ReturnExistingChat() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        // When
        Chat result = chatService.getOrCreateChat(outingId);

        // Then
        assertNotNull(result);
        assertEquals(chat.getId(), result.getId());
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void testGetOrCreateChat_WhenChatDoesNotExist_CreateNewChat() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // When
        Chat result = chatService.getOrCreateChat(outingId);

        // Then
        assertNotNull(result);
        verify(chatRepository, times(1)).save(any(Chat.class));
    }

    @Test
    void testSendMessage_AsOrganizer_Success() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello from organizer!");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(organizer);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        ChatMessageResponse result = chatService.sendMessage(outingId, request, organizerId);

        // Then
        assertNotNull(result);
        assertEquals(request.getContent(), result.getContent());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_AsParticipant_Success() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello from participant!");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(true);
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(user);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        ChatMessageResponse result = chatService.sendMessage(outingId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals(request.getContent(), result.getContent());
    }

    @Test
    void testSendMessage_AsNonParticipant_ThrowsException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Unauthorized message");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.sendMessage(outingId, request, userId);
        });

        assertTrue(exception.getMessage().contains("Only participants and organizer can send messages"));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_ToInactiveChat_ThrowsException() {
        // Given
        chat.setActive(false);
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Message to inactive chat");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.sendMessage(outingId, request, organizerId);
        });

        assertTrue(exception.getMessage().contains("chat has been deactivated"));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void testGetMessages_AsOrganizer_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        List<ChatMessage> messages = new ArrayList<>();
        when(chatMessageRepository.findByChatOrderByTimestampAsc(chat)).thenReturn(messages);

        // When
        List<ChatMessageResponse> result = chatService.getMessages(outingId, organizerId);

        // Then
        assertNotNull(result);
        verify(chatMessageRepository, times(1)).findByChatOrderByTimestampAsc(chat);
    }

    @Test
    void testGetMessages_AsNonParticipant_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.getMessages(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Only participants and organizer can view messages"));
        verify(chatMessageRepository, never()).findByChatOrderByTimestampAsc(any());
    }

    @Test
    void testGetMessages_AsParticipant_Success() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(true);
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        List<ChatMessage> messages = new ArrayList<>();
        when(chatMessageRepository.findByChatOrderByTimestampAsc(chat)).thenReturn(messages);

        // When
        List<ChatMessageResponse> result = chatService.getMessages(outingId, userId);

        // Then
        assertNotNull(result);
        verify(chatMessageRepository, times(1)).findByChatOrderByTimestampAsc(chat);
        verify(participationRepository).existsByUserAndOuting(user, outing);
    }

    @Test
    void testGetOrCreateChat_OutingNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.getOrCreateChat(outingId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testSendMessage_OutingNotFound_ThrowsException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello");

        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.sendMessage(outingId, request, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_UserNotFound_ThrowsException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.sendMessage(outingId, request, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void testGetMessages_OutingNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.getMessages(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("Outing not found"));
        verify(chatMessageRepository, never()).findByChatOrderByTimestampAsc(any());
    }

    @Test
    void testGetMessages_UserNotFound_ThrowsException() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatService.getMessages(outingId, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(chatMessageRepository, never()).findByChatOrderByTimestampAsc(any());
    }

    @Test
    void testGetMessages_CreatesChatIfNotExists() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing))
                .thenReturn(Optional.empty())  // First call in getMessages
                .thenReturn(Optional.empty());  // Second call in getOrCreateChat
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        List<ChatMessage> messages = new ArrayList<>();
        when(chatMessageRepository.findByChatOrderByTimestampAsc(chat)).thenReturn(messages);

        // When
        List<ChatMessageResponse> result = chatService.getMessages(outingId, organizerId);

        // Then
        assertNotNull(result);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void testSendMessage_CreatesChatIfNotExists() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("First message!");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing))
                .thenReturn(Optional.empty())  // First call in sendMessage
                .thenReturn(Optional.empty());  // Second call in getOrCreateChat
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(organizer);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        ChatMessageResponse result = chatService.sendMessage(outingId, request, organizerId);

        // Then
        assertNotNull(result);
        verify(chatRepository).save(any(Chat.class));
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_WithParticipants_SendsNotifications() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Hello everyone!");

        User participant1 = new User();
        participant1.setId(UUID.randomUUID());
        participant1.setName("Participant 1");

        User participant2 = new User();
        participant2.setId(UUID.randomUUID());
        participant2.setName("Participant 2");

        // Create participations using Participation.builder()
        com.alfano.gathorapp.participation.Participation participation1 =
            com.alfano.gathorapp.participation.Participation.builder()
                .user(participant1)
                .outing(outing)
                .build();

        com.alfano.gathorapp.participation.Participation participation2 =
            com.alfano.gathorapp.participation.Participation.builder()
                .user(participant2)
                .outing(outing)
                .build();

        List<com.alfano.gathorapp.participation.Participation> participations =
            List.of(participation1, participation2);

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(true);
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(user);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(participations);

        // When
        chatService.sendMessage(outingId, request, userId);

        // Then
        // Should notify 2 participants + organizer (3 total, sender excluded)
        verify(notificationService, times(3)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSendMessage_AsOrganizer_NotifiesAllParticipants() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Message from organizer!");

        User participant = new User();
        participant.setId(UUID.randomUUID());
        participant.setName("Participant");

        com.alfano.gathorapp.participation.Participation participation =
            com.alfano.gathorapp.participation.Participation.builder()
                .user(participant)
                .outing(outing)
                .build();

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(organizer);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(List.of(participation));

        // When
        chatService.sendMessage(outingId, request, organizerId);

        // Then
        // Should notify only the participant (organizer is sender, so excluded)
        verify(notificationService, times(1)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSendMessage_WithNoParticipants_NotifiesOnlyOrganizer() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Message from participant!");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndOuting(user, outing)).thenReturn(true);
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(request.getContent());
        savedMessage.setSender(user);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(request.getContent());
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        chatService.sendMessage(outingId, request, userId);

        // Then
        // Should notify only organizer (sender is participant)
        verify(notificationService, times(1)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetMessages_WithMultipleMessages_ReturnsSorted() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage msg1 = new ChatMessage();
        msg1.setId(UUID.randomUUID());
        msg1.setContent("First");

        ChatMessage msg2 = new ChatMessage();
        msg2.setId(UUID.randomUUID());
        msg2.setContent("Second");

        List<ChatMessage> messages = List.of(msg1, msg2);
        when(chatMessageRepository.findByChatOrderByTimestampAsc(chat)).thenReturn(messages);

        ChatMessageResponse response1 = new ChatMessageResponse();
        response1.setContent("First");
        ChatMessageResponse response2 = new ChatMessageResponse();
        response2.setContent("Second");

        when(chatMapper.toMessageResponse(msg1)).thenReturn(response1);
        when(chatMapper.toMessageResponse(msg2)).thenReturn(response2);

        // When
        List<ChatMessageResponse> result = chatService.getMessages(outingId, organizerId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getContent());
        assertEquals("Second", result.get(1).getContent());
    }

    @Test
    void testGetMessages_EmptyChat_ReturnsEmptyList() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        when(chatMessageRepository.findByChatOrderByTimestampAsc(chat)).thenReturn(new ArrayList<>());

        // When
        List<ChatMessageResponse> result = chatService.getMessages(outingId, organizerId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSendMessage_EmptyContent_SavesSuccessfully() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("");

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent("");
        savedMessage.setSender(organizer);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent("");
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        ChatMessageResponse result = chatService.sendMessage(outingId, request, organizerId);

        // Then
        assertNotNull(result);
        assertEquals("", result.getContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_LongContent_SavesSuccessfully() {
        // Given
        String longContent = "A".repeat(1000);
        SendMessageRequest request = new SendMessageRequest();
        request.setContent(longContent);

        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.of(chat));

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setContent(longContent);
        savedMessage.setSender(organizer);
        savedMessage.setChat(chat);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setContent(longContent);
        when(chatMapper.toMessageResponse(savedMessage)).thenReturn(response);

        when(participationRepository.findApprovedByOuting(outing)).thenReturn(new ArrayList<>());

        // When
        ChatMessageResponse result = chatService.sendMessage(outingId, request, organizerId);

        // Then
        assertNotNull(result);
        assertEquals(longContent, result.getContent());
    }

    @Test
    void testGetOrCreateChat_CreatesNewChatWithCorrectProperties() {
        // Given
        when(outingRepository.findById(outingId)).thenReturn(Optional.of(outing));
        when(chatRepository.findByOuting(outing)).thenReturn(Optional.empty());

        Chat newChat = new Chat();
        newChat.setId(UUID.randomUUID());
        newChat.setOuting(outing);
        newChat.setActive(true);

        when(chatRepository.save(any(Chat.class))).thenReturn(newChat);

        // When
        Chat result = chatService.getOrCreateChat(outingId);

        // Then
        assertNotNull(result);
        assertEquals(true, result.getActive());
        verify(chatRepository).save(any(Chat.class));
    }
}
