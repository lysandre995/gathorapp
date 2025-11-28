package com.alfano.gathorapp.admin;

import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for administrative operations.
 *
 * Only accessible by users with ADMIN role.
 *
 * Endpoints:
 * - GET /api/admin/users → List all users
 * - PUT /api/admin/users/{id}/role → Change user role
 * - DELETE /api/admin/users/{id} → Delete user
 * - GET /api/admin/stats → Get application statistics
 * - POST /api/admin/users/{id}/ban → Ban a user
 * - POST /api/admin/users/{id}/unban → Unban a user
 */
@Tag(name = "Admin", description = "Administrative APIs for user management and moderation")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/users
     * Get all users in the system.
     */
    @Operation(summary = "List all users", description = "Get a list of all registered users (ADMIN only)")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/admin/users - Listing all users");

        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * PUT /api/admin/users/{id}/role
     * Change a user's role.
     */
    @Operation(summary = "Change user role", description = "Update a user's role (USER, PREMIUM, BUSINESS, ADMIN)")
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable("id") UUID id,
            @RequestParam("newRole") Role newRole) {
        log.info("PUT /api/admin/users/{}/role - Changing to: {}", id, newRole);

        UserResponse user = adminService.changeUserRole(id, newRole);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/admin/users/{id}
     * Delete a user from the system.
     */
    @Operation(summary = "Delete user", description = "Permanently delete a user and all associated data")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        log.info("DELETE /api/admin/users/{} - Deleting user", id);

        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/admin/users/{id}/ban
     * Ban a user (prevent login and participation).
     */
    @Operation(summary = "Ban user", description = "Ban a user from the platform")
    @PostMapping("/users/{id}/ban")
    public ResponseEntity<UserResponse> banUser(
            @PathVariable("id") UUID id,
            @RequestParam(name = "reason", required = false) String reason) {
        log.info("POST /api/admin/users/{}/ban - Reason: {}", id, reason);

        UserResponse user = adminService.banUser(id, reason);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/admin/users/{id}/unban
     * Unban a previously banned user.
     */
    @Operation(summary = "Unban user", description = "Remove ban from a user")
    @PostMapping("/users/{id}/unban")
    public ResponseEntity<UserResponse> unbanUser(@PathVariable("id") UUID id) {
        log.info("POST /api/admin/users/{}/unban - Unbanning user", id);

        UserResponse user = adminService.unbanUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/admin/stats
     * Get application-wide statistics.
     */
    @Operation(summary = "Get statistics", description = "Get platform statistics (users, events, outings, etc.)")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET /api/admin/stats - Fetching statistics");

        Map<String, Object> stats = adminService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/health
     * Health check endpoint for system monitoring.
     */
    @Operation(summary = "Health check", description = "Check system health status")
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        log.debug("GET /api/admin/health - Health check");

        Map<String, String> health = Map.of(
            "status", "UP",
            "timestamp", java.time.LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(health);
    }

    /**
     * POST /api/admin/cleanup/expired-chats
     * Manually trigger expired chat cleanup.
     */
    @Operation(summary = "Cleanup expired chats", description = "Manually trigger cleanup of expired chat conversations")
    @PostMapping("/cleanup/expired-chats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> cleanupExpiredChats() {
        log.info("POST /api/admin/cleanup/expired-chats - Manual cleanup triggered");

        int cleanedCount = adminService.cleanupExpiredChats();

        return ResponseEntity.ok(Map.of("cleaned", cleanedCount));
    }

    /**
     * POST /api/admin/cleanup/expired-vouchers
     * Manually trigger expired voucher cleanup.
     */
    @Operation(summary = "Cleanup expired vouchers", description = "Manually trigger cleanup of expired vouchers")
    @PostMapping("/cleanup/expired-vouchers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> cleanupExpiredVouchers() {
        log.info("POST /api/admin/cleanup/expired-vouchers - Manual cleanup triggered");

        int cleanedCount = adminService.cleanupExpiredVouchers();

        return ResponseEntity.ok(Map.of("cleaned", cleanedCount));
    }
}
