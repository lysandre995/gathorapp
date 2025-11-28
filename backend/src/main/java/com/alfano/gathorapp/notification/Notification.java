package com.alfano.gathorapp.notification;

import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a notification sent to a user.
 * Notifications are created for important events like:
 * - New participation requests
 * - Participation approval/rejection
 * - New messages in chat
 * - Upcoming outing reminders
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_read", columnList = "read"),
        @Index(name = "idx_notification_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who receives this notification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Type of notification (determines icon, color, action).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Notification title.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Notification message.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Optional: ID of related entity (e.g., outing ID, participation ID).
     */
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    /**
     * Optional: Type of related entity.
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * Whether the user has read this notification.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean read = false;

    /**
     * When the notification was read.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Mark this notification as read.
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
