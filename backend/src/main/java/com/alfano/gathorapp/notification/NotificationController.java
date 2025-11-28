package com.alfano.gathorapp.notification;

import com.alfano.gathorapp.notification.dto.NotificationResponse;
import com.alfano.gathorapp.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for notification management.
 * 
 * Endpoints:
 * - GET /api/notifications → Get all notifications for current user
 * - GET /api/notifications/unread → Get unread notifications
 * - GET /api/notifications/unread/count → Get unread count
 * - PUT /api/notifications/{id}/read → Mark notification as read
 * - PUT /api/notifications/read-all → Mark all as read
 * - DELETE /api/notifications/{id} → Delete notification
 */
@Tag(name = "Notifications", description = "Real-time notification management APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * Get all notifications for the authenticated user.
     */
    @Operation(summary = "Get all notifications", description = "Get all notifications for the authenticated user, ordered by creation date")
    @ApiResponse(responseCode = "200", description = "List of notifications")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/notifications - User: {}", userId);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread
     * Get unread notifications for the authenticated user.
     */
    @Operation(summary = "Get unread notifications", description = "Get only unread notifications for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of unread notifications")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/notifications/unread - User: {}", userId);
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread/count
     * Get count of unread notifications.
     */
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications")
    @ApiResponse(responseCode = "200", description = "Count of unread notifications")
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.debug("GET /api/notifications/unread/count - User: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a notification as read.
     */
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Notification belongs to another user")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("PUT /api/notifications/{}/read - User: {}", id, userId);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read.
     */
    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications of the authenticated user as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("PUT /api/notifications/read-all - User: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/notifications/{id}
     * Delete a notification.
     */
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Notification belongs to another user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("DELETE /api/notifications/{} - User: {}", id, userId);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}
