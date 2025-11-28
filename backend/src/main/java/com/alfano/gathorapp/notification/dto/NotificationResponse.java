package com.alfano.gathorapp.notification.dto;

import com.alfano.gathorapp.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Notification response.
 * Used for both REST API and WebSocket messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private UUID relatedEntityId;
    private String relatedEntityType;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
