package com.alfano.gathorapp.user;

import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.dto.CreateUserRequest;
import com.alfano.gathorapp.user.dto.UpdateUserRequest;
import com.alfano.gathorapp.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private SecurityUser securityUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        userResponse = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/users - Should return all users")
    void getAllUsers_ReturnsAllUsers() {
        // Given
        List<UserResponse> mockUsers = new ArrayList<>();
        mockUsers.add(userResponse);
        mockUsers.add(UserResponse.builder()
                .id(UUID.randomUUID())
                .name("Another User")
                .email("another@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        when(userService.getAllUsers()).thenReturn(mockUsers);

        // When
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/users - Should return empty list when no users")
    void getAllUsers_NoUsers_ReturnsEmptyList() {
        // Given
        when(userService.getAllUsers()).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return user by ID")
    void getUserById_ExistingId_ReturnsUser() {
        // Given
        when(userService.getUserById(userId)).thenReturn(userResponse);

        // When
        ResponseEntity<UserResponse> response = userController.getUserById(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        assertEquals("Test User", response.getBody().getName());
        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should handle different user IDs")
    void getUserById_DifferentIds_ReturnsDifferentUsers() {
        // Given
        UUID userId2 = UUID.randomUUID();
        UserResponse user2 = UserResponse.builder()
                .id(userId2)
                .name("Second User")
                .email("second@example.com")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(userId)).thenReturn(userResponse);
        when(userService.getUserById(userId2)).thenReturn(user2);

        // When
        ResponseEntity<UserResponse> response1 = userController.getUserById(userId);
        ResponseEntity<UserResponse> response2 = userController.getUserById(userId2);

        // Then
        assertEquals("Test User", response1.getBody().getName());
        assertEquals("Second User", response2.getBody().getName());
        assertEquals(Role.USER, response1.getBody().getRole());
        assertEquals(Role.PREMIUM, response2.getBody().getRole());
        verify(userService, times(1)).getUserById(userId);
        verify(userService, times(1)).getUserById(userId2);
    }

    @Test
    @DisplayName("POST /api/users - Should create user successfully")
    void createUser_ValidRequest_CreatesUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New User")
                .email("new@example.com")
                .role(Role.USER)
                .build();

        UserResponse newUser = UserResponse.builder()
                .id(UUID.randomUUID())
                .name("New User")
                .email("new@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.createUser(request)).thenReturn(newUser);

        // When
        ResponseEntity<UserResponse> response = userController.createUser(request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("New User", response.getBody().getName());
        assertEquals("new@example.com", response.getBody().getEmail());
        verify(userService, times(1)).createUser(request);
    }

    @Test
    @DisplayName("POST /api/users - Should create users with different roles")
    void createUser_DifferentRoles_CreatesUsersCorrectly() {
        // Given
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("Basic User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        CreateUserRequest premiumRequest = CreateUserRequest.builder()
                .name("Premium User")
                .email("premium@example.com")
                .role(Role.PREMIUM)
                .build();

        UserResponse basicUser = UserResponse.builder()
                .id(UUID.randomUUID())
                .name("Basic User")
                .email("user@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse premiumUser = UserResponse.builder()
                .id(UUID.randomUUID())
                .name("Premium User")
                .email("premium@example.com")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.createUser(userRequest)).thenReturn(basicUser);
        when(userService.createUser(premiumRequest)).thenReturn(premiumUser);

        // When
        ResponseEntity<UserResponse> response1 = userController.createUser(userRequest);
        ResponseEntity<UserResponse> response2 = userController.createUser(premiumRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertEquals(Role.USER, response1.getBody().getRole());
        assertEquals(Role.PREMIUM, response2.getBody().getRole());
        verify(userService, times(1)).createUser(userRequest);
        verify(userService, times(1)).createUser(premiumRequest);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Should delete user successfully")
    void deleteUser_ExistingId_DeletesUser() {
        // Given
        doNothing().when(userService).deleteUser(userId);

        // When
        ResponseEntity<Void> response = userController.deleteUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Should handle different user IDs")
    void deleteUser_DifferentIds_DeletesEachUser() {
        // Given
        UUID userId2 = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);
        doNothing().when(userService).deleteUser(userId2);

        // When
        ResponseEntity<Void> response1 = userController.deleteUser(userId);
        ResponseEntity<Void> response2 = userController.deleteUser(userId2);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response2.getStatusCode());
        verify(userService, times(1)).deleteUser(userId);
        verify(userService, times(1)).deleteUser(userId2);
    }

    @Test
    @DisplayName("GET /api/users/me - Should return current user")
    void getCurrentUser_AuthenticatedUser_ReturnsCurrentUser() {
        // Given
        when(userService.getUserById(userId)).thenReturn(userResponse);

        // When
        ResponseEntity<UserResponse> response = userController.getCurrentUser(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        assertEquals("Test User", response.getBody().getName());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/me - Should handle different authenticated users")
    void getCurrentUser_DifferentUsers_ReturnsCorrectUser() {
        // Given
        UUID userId2 = UUID.randomUUID();
        User user2 = User.builder()
                .id(userId2)
                .name("Second User")
                .email("second@example.com")
                .passwordHash("hashedPassword")
                .role(Role.PREMIUM)
                .build();
        SecurityUser securityUser2 = new SecurityUser(user2);

        UserResponse userResponse2 = UserResponse.builder()
                .id(userId2)
                .name("Second User")
                .email("second@example.com")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(userId)).thenReturn(userResponse);
        when(userService.getUserById(userId2)).thenReturn(userResponse2);

        // When
        ResponseEntity<UserResponse> response1 = userController.getCurrentUser(securityUser);
        ResponseEntity<UserResponse> response2 = userController.getCurrentUser(securityUser2);

        // Then
        assertEquals("Test User", response1.getBody().getName());
        assertEquals("Second User", response2.getBody().getName());
        assertEquals(Role.USER, response1.getBody().getRole());
        assertEquals(Role.PREMIUM, response2.getBody().getRole());
        verify(userService, times(1)).getUserById(userId);
        verify(userService, times(1)).getUserById(userId2);
    }

    @Test
    @DisplayName("PUT /api/users/me - Should update current user profile")
    void updateCurrentUser_ValidRequest_UpdatesProfile() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        UserResponse updatedUser = UserResponse.builder()
                .id(userId)
                .name("Updated Name")
                .email("updated@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(userId, request)).thenReturn(updatedUser);

        // When
        ResponseEntity<UserResponse> response = userController.updateCurrentUser(request, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Name", response.getBody().getName());
        assertEquals("updated@example.com", response.getBody().getEmail());
        verify(userService, times(1)).updateUser(userId, request);
    }

    @Test
    @DisplayName("PUT /api/users/me - Should update only name")
    void updateCurrentUser_OnlyName_UpdatesNameOnly() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("New Name")
                .build();

        UserResponse updatedUser = UserResponse.builder()
                .id(userId)
                .name("New Name")
                .email("test@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(userId, request)).thenReturn(updatedUser);

        // When
        ResponseEntity<UserResponse> response = userController.updateCurrentUser(request, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New Name", response.getBody().getName());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService, times(1)).updateUser(userId, request);
    }

    @Test
    @DisplayName("PUT /api/users/me - Should update only email")
    void updateCurrentUser_OnlyEmail_UpdatesEmailOnly() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("newemail@example.com")
                .build();

        UserResponse updatedUser = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("newemail@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(userId, request)).thenReturn(updatedUser);

        // When
        ResponseEntity<UserResponse> response = userController.updateCurrentUser(request, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test User", response.getBody().getName());
        assertEquals("newemail@example.com", response.getBody().getEmail());
        verify(userService, times(1)).updateUser(userId, request);
    }

    @Test
    @DisplayName("PUT /api/users/me - Should update with new password")
    void updateCurrentUser_WithNewPassword_UpdatesPassword() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Test User")
                .newPassword("newSecurePassword123")
                .build();

        UserResponse updatedUser = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(userId, request)).thenReturn(updatedUser);

        // When
        ResponseEntity<UserResponse> response = userController.updateCurrentUser(request, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).updateUser(userId, request);
    }

    @Test
    @DisplayName("POST /api/users/me/upgrade - Should upgrade to PREMIUM")
    void upgradeAccount_ToPremium_UpgradesSuccessfully() {
        // Given
        UserResponse upgradedUser = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.upgradeUserAccount(userId, Role.PREMIUM)).thenReturn(upgradedUser);

        // When
        ResponseEntity<UserResponse> response = userController.upgradeAccount(Role.PREMIUM, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Role.PREMIUM, response.getBody().getRole());
        verify(userService, times(1)).upgradeUserAccount(userId, Role.PREMIUM);
    }

    @Test
    @DisplayName("POST /api/users/me/upgrade - Should upgrade to BUSINESS")
    void upgradeAccount_ToBusiness_UpgradesSuccessfully() {
        // Given
        UserResponse upgradedUser = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.BUSINESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.upgradeUserAccount(userId, Role.BUSINESS)).thenReturn(upgradedUser);

        // When
        ResponseEntity<UserResponse> response = userController.upgradeAccount(Role.BUSINESS, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Role.BUSINESS, response.getBody().getRole());
        verify(userService, times(1)).upgradeUserAccount(userId, Role.BUSINESS);
    }

    @Test
    @DisplayName("POST /api/users/me/upgrade - Should handle different users upgrading")
    void upgradeAccount_DifferentUsers_UpgradesEachCorrectly() {
        // Given
        UUID userId2 = UUID.randomUUID();
        User user2 = User.builder()
                .id(userId2)
                .name("Second User")
                .email("second@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();
        SecurityUser securityUser2 = new SecurityUser(user2);

        UserResponse upgradedUser1 = UserResponse.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse upgradedUser2 = UserResponse.builder()
                .id(userId2)
                .name("Second User")
                .email("second@example.com")
                .role(Role.BUSINESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.upgradeUserAccount(userId, Role.PREMIUM)).thenReturn(upgradedUser1);
        when(userService.upgradeUserAccount(userId2, Role.BUSINESS)).thenReturn(upgradedUser2);

        // When
        ResponseEntity<UserResponse> response1 = userController.upgradeAccount(Role.PREMIUM, securityUser);
        ResponseEntity<UserResponse> response2 = userController.upgradeAccount(Role.BUSINESS, securityUser2);

        // Then
        assertEquals(Role.PREMIUM, response1.getBody().getRole());
        assertEquals(Role.BUSINESS, response2.getBody().getRole());
        verify(userService, times(1)).upgradeUserAccount(userId, Role.PREMIUM);
        verify(userService, times(1)).upgradeUserAccount(userId2, Role.BUSINESS);
    }
}
