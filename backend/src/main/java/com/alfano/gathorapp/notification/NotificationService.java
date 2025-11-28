package com.alfano.gathorapp.notification;

import com.alfano.gathorapp.notification.dto.NotificationResponse;
import com.alfano.gathorapp.pattern.observer.NotificationManager;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing notifications.
 * Uses Observer Pattern to send notifications via multiple channels.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationManager notificationManager;

    /**
     * Create and send a notification to a user.
     * The notification will be:
     * 1. Saved to database (via PersistenceNotificationObserver)
     * 2. Sent via WebSocket if user is online (via WebSocketNotificationObserver)
     * 
     * @param userId            user to notify
     * @param type              type of notification
     * @param title             notification title
     * @param message           notification message
     * @param relatedEntityId   optional related entity ID
     * @param relatedEntityType optional related entity type
     */
    @Transactional
    public void createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID relatedEntityId,
            String relatedEntityType) {

        log.info("Creating notification for user {}: {}", userId, type);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .read(false)
                .build();

        // Notify all observers (Observer Pattern)
        // This will automatically:
        // 1. Save to database
        // 2. Send via WebSocket (if user is online)
        notificationManager.notifyObservers(notification);

        log.debug("Notification created and sent: {}", notification.getId());
    }

    /**
     * Convenience method for creating notification without related entity.
     */
    public void createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message) {
        createNotification(userId, type, title, message, null, null);
    }

    /**
     * Get all notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        log.debug("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        log.debug("Fetching unread notifications for user: {}", userId);
        return notificationRepository.findUnreadByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        log.debug("Marking notification {} as read for user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to user");
        }

        if (!notification.getRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    /**
     * Delete a notification.
     */
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        log.debug("Deleting notification {} for user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to user");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Convert Notification entity to NotificationResponse DTO.
     */
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
