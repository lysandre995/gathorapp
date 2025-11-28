package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;
import com.alfano.gathorapp.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern - Concrete Observer.
 * 
 * This observer sends notifications to users via WebSocket in real-time.
 * When a notification is created, it's immediately pushed to the user's
 * WebSocket connection if they're online.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationObserver implements NotificationObserver {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onNotification(Notification notification) {
        try {
            // Convert to DTO
            NotificationResponse response = NotificationResponse.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .relatedEntityId(notification.getRelatedEntityId())
                    .relatedEntityType(notification.getRelatedEntityType())
                    .read(notification.getRead())
                    .createdAt(notification.getCreatedAt())
                    .build();

            // Send to specific user via WebSocket
            // Destination: /user/{userId}/queue/notifications
            String destination = "/queue/notifications";
            String userId = notification.getUser().getId().toString();

            messagingTemplate.convertAndSendToUser(
                    userId,
                    destination,
                    response);

            log.debug("Sent notification {} to user {} via WebSocket",
                    notification.getId(), userId);

        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
            // Don't throw - let other observers continue
        }
    }

    @Override
    public String getName() {
        return "WebSocketNotificationObserver";
    }
}
