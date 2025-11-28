package com.alfano.gathorapp.user;

import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.dto.CreateUserRequest;
import com.alfano.gathorapp.user.dto.UpdateUserRequest;
import com.alfano.gathorapp.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller per la gestione degli utenti.
 * 
 * Endpoints:
 * - GET /api/users → Lista tutti gli utenti
 * - GET /api/users/{id} → Dettaglio utente
 * - POST /api/users → Crea nuovo utente
 * - DELETE /api/users/{id} → Elimina utente
 */

@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * Get list of all users.
     */

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of users")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users - Richiesta lista utenti");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id}
     * Get a single user by ID.
     */
    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") UUID id) {
        log.info("GET /api/users/{} - Richiesta dettaglio utente", id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users
     * Create new user.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users - Creazione utente: {}", request.getEmail());
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * DELETE /api/users/{id}
     * Delete user.
     */
    @Operation(summary = "Delete user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        log.info("DELETE /api/users/{} - Eliminazione utente", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/users/me
     * Get current user.
     */
    @Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user profile", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal SecurityUser securityUser) {
        UserResponse user = userService.getUserById(securityUser.getUserId());
        return ResponseEntity.ok(user);
    }

    /**
     * 
     * PUT /api/users/me
     * Update current user profile
     */
    @Operation(summary = "Update current user profile", description = "Update name, email, or password of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already in use")
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UserResponse user = userService.updateUser(securityUser.getUserId(), request);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users/me/upgrade
     * Upgrade current user account to PREMIUM or BUSINESS.
     * This is a test/demo endpoint for university project.
     */
    @Operation(summary = "Upgrade user account", description = "Upgrade from USER to PREMIUM or BUSINESS role (test/demo feature)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account upgraded successfully", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role or user already has premium account")
    })
    @PostMapping("/me/upgrade")
    public ResponseEntity<UserResponse> upgradeAccount(
            @RequestParam("newRole") Role newRole,
            @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("POST /api/users/me/upgrade - User {} upgrading to {}", securityUser.getUserId(), newRole);
        UserResponse user = userService.upgradeUserAccount(securityUser.getUserId(), newRole);
        return ResponseEntity.ok(user);
    }
}
