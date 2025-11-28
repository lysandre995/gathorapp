package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.outing.Outing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatDeactivationScheduler.
 * Uses Mockito to test scheduled task logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDeactivationScheduler Tests")
class ChatDeactivationSchedulerTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatDeactivationScheduler chatDeactivationScheduler;

    private UUID chatId;
    private UUID outingId;
    private Chat chat;
    private Outing outing;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        outingId = UUID.randomUUID();

        outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .outingDate(LocalDateTime.now().minusDays(10))
                .build();

        chat = Chat.builder()
                .id(chatId)
                .outing(outing)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("deactivateExpiredChats - Should deactivate expired chats")
    void deactivateExpiredChats_WithExpiredChats_DeactivatesThemSuccessfully() {
        // Given
        List<Chat> expiredChats = new ArrayList<>();
        expiredChats.add(chat);

        when(chatRepository.findActiveChatsWithExpiredOutings(any(LocalDateTime.class)))
                .thenReturn(expiredChats);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // When
        chatDeactivationScheduler.deactivateExpiredChats();

        // Then
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(chatRepository, times(1)).findActiveChatsWithExpiredOutings(dateCaptor.capture());

        // Verify the expiration date is approximately 7 days ago (within 1 minute tolerance)
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusDays(7);
        assertTrue(capturedDate.isAfter(expectedDate.minusMinutes(1)));
        assertTrue(capturedDate.isBefore(expectedDate.plusMinutes(1)));

        verify(chatRepository, times(1)).save(chat);
        assertFalse(chat.getActive(), "Chat should be deactivated");
    }

    @Test
    @DisplayName("deactivateExpiredChats - Should handle empty list gracefully")
    void deactivateExpiredChats_NoExpiredChats_DoesNothing() {
        // Given
        when(chatRepository.findActiveChatsWithExpiredOutings(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        chatDeactivationScheduler.deactivateExpiredChats();

        // Then
        verify(chatRepository, times(1)).findActiveChatsWithExpiredOutings(any(LocalDateTime.class));
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("deactivateExpiredChats - Should deactivate multiple chats")
    void deactivateExpiredChats_MultipleExpiredChats_DeactivatesAll() {
        // Given
        List<Chat> expiredChats = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Outing pastOuting = Outing.builder()
                    .id(UUID.randomUUID())
                    .title("Past Outing " + i)
                    .outingDate(LocalDateTime.now().minusDays(10 + i))
                    .build();

            Chat expiredChat = Chat.builder()
                    .id(UUID.randomUUID())
                    .outing(pastOuting)
                    .active(true)
                    .build();

            expiredChats.add(expiredChat);
        }

        when(chatRepository.findActiveChatsWithExpiredOutings(any(LocalDateTime.class)))
                .thenReturn(expiredChats);
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        chatDeactivationScheduler.deactivateExpiredChats();

        // Then
        verify(chatRepository, times(5)).save(any(Chat.class));

        for (Chat expiredChat : expiredChats) {
            assertFalse(expiredChat.getActive(), "All chats should be deactivated");
        }
    }

    @Test
    @DisplayName("deactivateChat - Should deactivate specific chat successfully")
    void deactivateChat_ValidChatId_DeactivatesChat() {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatRepository.save(chat)).thenReturn(chat);

        // When
        chatDeactivationScheduler.deactivateChat(chatId);

        // Then
        verify(chatRepository, times(1)).findById(chatId);
        verify(chatRepository, times(1)).save(chat);
        assertFalse(chat.getActive(), "Chat should be deactivated");
    }

    @Test
    @DisplayName("deactivateChat - Should throw exception for non-existent chat")
    void deactivateChat_NonExistentChat_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(chatRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When / Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatDeactivationScheduler.deactivateChat(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Chat not found with id"));
        verify(chatRepository, times(1)).findById(nonExistentId);
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("deactivateChat - Should handle already deactivated chat")
    void deactivateChat_AlreadyDeactivated_DoesNotSaveAgain() {
        // Given
        chat.setActive(false); // Chat is already inactive
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // When
        chatDeactivationScheduler.deactivateChat(chatId);

        // Then
        verify(chatRepository, times(1)).findById(chatId);
        verify(chatRepository, never()).save(any(Chat.class));
        assertFalse(chat.getActive(), "Chat should remain deactivated");
    }

    @Test
    @DisplayName("deactivateExpiredChats - Should use correct grace period")
    void deactivateExpiredChats_UsesSevenDayGracePeriod() {
        // Given
        when(chatRepository.findActiveChatsWithExpiredOutings(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        chatDeactivationScheduler.deactivateExpiredChats();

        // Then
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(chatRepository, times(1)).findActiveChatsWithExpiredOutings(dateCaptor.capture());

        // Verify it's approximately 7 days ago (within 1 minute tolerance)
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long minutesDifference = java.time.Duration.between(capturedDate, sevenDaysAgo).toMinutes();
        assertTrue(Math.abs(minutesDifference) < 1,
                "Grace period should be 7 days, but was off by " + minutesDifference + " minutes");
    }

    @Test
    @DisplayName("deactivateChat - Should deactivate different chats independently")
    void deactivateChat_DifferentChats_DeactivatesEachIndependently() {
        // Given
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

        when(chatRepository.findById(chatId1)).thenReturn(Optional.of(chat1));
        when(chatRepository.findById(chatId2)).thenReturn(Optional.of(chat2));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        chatDeactivationScheduler.deactivateChat(chatId1);
        chatDeactivationScheduler.deactivateChat(chatId2);

        // Then
        verify(chatRepository, times(1)).findById(chatId1);
        verify(chatRepository, times(1)).findById(chatId2);
        verify(chatRepository, times(2)).save(any(Chat.class));

        assertFalse(chat1.getActive());
        assertFalse(chat2.getActive());
    }
}
