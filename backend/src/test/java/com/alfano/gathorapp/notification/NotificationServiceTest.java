package com.alfano.gathorapp.notification;

import com.alfano.gathorapp.notification.dto.NotificationResponse;
import com.alfano.gathorapp.pattern.observer.NotificationManager;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 *
 * Tests cover:
 * - Notification creation and Observer pattern integration
 * - Notification retrieval (all, unread, count)
 * - Mark as read (single and bulk)
 * - Notification deletion
 * - Access control and authorization
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationManager notificationManager;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        notification = Notification.builder()
                .id(notificationId)
                .user(user)
                .type(NotificationType.PARTICIPATION_REQUEST)
                .title("Test Notification")
                .message("Test Message")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateNotification_Success() {
        // Given
        UUID relatedEntityId = UUID.randomUUID();
        String relatedEntityType = "OUTING";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(notificationManager).notifyObservers(any(Notification.class));

        // When
        notificationService.createNotification(
                userId,
                NotificationType.PARTICIPATION_REQUEST,
                "Test Title",
                "Test Message",
                relatedEntityId,
                relatedEntityType
        );

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(notificationManager, times(1)).notifyObservers(any(Notification.class));
    }

    @Test
    void testCreateNotification_WithoutRelatedEntity_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(notificationManager).notifyObservers(any(Notification.class));

        // When
        notificationService.createNotification(
                userId,
                NotificationType.SYSTEM,
                "System Notification",
                "System Message"
        );

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(notificationManager, times(1)).notifyObservers(any(Notification.class));
    }

    @Test
    void testCreateNotification_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.createNotification(
                    userId,
                    NotificationType.SYSTEM,
                    "Test",
                    "Test"
            );
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(notificationManager, never()).notifyObservers(any(Notification.class));
    }

    @Test
    void testGetUserNotifications_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(notification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        // When
        List<NotificationResponse> result = notificationService.getUserNotifications(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(notificationId, result.get(0).getId());
        assertEquals("Test Notification", result.get(0).getTitle());
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void testGetUnreadNotifications_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(notification);
        when(notificationRepository.findUnreadByUserId(userId))
                .thenReturn(notifications);

        // When
        List<NotificationResponse> result = notificationService.getUnreadNotifications(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getRead());
        verify(notificationRepository, times(1)).findUnreadByUserId(userId);
    }

    @Test
    void testGetUnreadCount_Success() {
        // Given
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount(userId);

        // Then
        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countUnreadByUserId(userId);
    }

    @Test
    void testMarkAsRead_Success() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        notificationService.markAsRead(notificationId, userId);

        // Then
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_AlreadyRead_NoSave() {
        // Given
        notification.setRead(true);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        notificationService.markAsRead(notificationId, userId);

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });

        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_UnauthorizedUser_ThrowsException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(notificationId, otherUserId);
        });

        assertTrue(exception.getMessage().contains("does not belong to user"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAllAsRead_Success() {
        // Given
        doNothing().when(notificationRepository).markAllAsReadByUserId(eq(userId), any(LocalDateTime.class));

        // When
        notificationService.markAllAsRead(userId);

        // Then
        verify(notificationRepository, times(1)).markAllAsReadByUserId(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void testDeleteNotification_Success() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(notification);

        // When
        notificationService.deleteNotification(notificationId, userId);

        // Then
        verify(notificationRepository, times(1)).delete(notification);
    }

    @Test
    void testDeleteNotification_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification(notificationId, userId);
        });

        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void testDeleteNotification_UnauthorizedUser_ThrowsException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.deleteNotification(notificationId, otherUserId);
        });

        assertTrue(exception.getMessage().contains("does not belong to user"));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void testToResponse_MapsAllFields() {
        // Given
        UUID relatedEntityId = UUID.randomUUID();
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType("OUTING");
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());

        List<Notification> notifications = Arrays.asList(notification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(notifications);

        // When
        List<NotificationResponse> result = notificationService.getUserNotifications(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        NotificationResponse response = result.get(0);
        assertEquals(notificationId, response.getId());
        assertEquals(NotificationType.PARTICIPATION_REQUEST, response.getType());
        assertEquals("Test Notification", response.getTitle());
        assertEquals("Test Message", response.getMessage());
        assertEquals(relatedEntityId, response.getRelatedEntityId());
        assertEquals("OUTING", response.getRelatedEntityType());
        assertTrue(response.getRead());
        assertNotNull(response.getReadAt());
        assertNotNull(response.getCreatedAt());
    }
}
