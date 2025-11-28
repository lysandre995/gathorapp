package com.alfano.gathorapp.user;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests all user management operations including role changes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
    }

    // ==================== getUserById Tests ====================

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        UserResponse expectedResponse = UserResponse.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .build();
        when(userMapper.toResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = userService.getUserById(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        verify(userRepository).findById(testUserId);
        verify(userMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(testUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non trovato");
    }

    // ==================== createUser Tests ====================

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New User")
                .email("new@example.com")
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);

        UserResponse expectedResponse = UserResponse.builder()
                .id(testUserId)
                .name("New User")
                .email("new@example.com")
                .build();
        when(userMapper.toResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = userService.createUser(request);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createUser_EmailExists() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("existing@example.com")
                .build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("già in uso");
    }

    // ==================== updateUser Tests ====================

    @Test
    @DisplayName("Should update user name successfully")
    void updateUser_UpdateName() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Updated Name")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.updateUser(testUserId, request);

        // Assert
        verify(userRepository).save(testUser);
        assertThat(testUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should update password when provided")
    void updateUser_UpdatePassword() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .newPassword("newPassword123")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.updateUser(testUserId, request);

        // Assert
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
    }

    // ==================== upgradeUserAccount Tests ====================
    // These tests cover the role change functionality (upgrade/downgrade)

    @Test
    @DisplayName("Should upgrade USER to PREMIUM successfully")
    void upgradeAccount_UserToPremium_Success() {
        // Arrange
        testUser.setRole(Role.USER);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.PREMIUM);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should upgrade USER to BUSINESS successfully")
    void upgradeAccount_UserToBusiness_Success() {
        // Arrange
        testUser.setRole(Role.USER);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.BUSINESS);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.BUSINESS);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should upgrade PREMIUM to BUSINESS successfully")
    void upgradeAccount_PremiumToBusiness_Success() {
        // Arrange
        testUser.setRole(Role.PREMIUM);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.BUSINESS);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.BUSINESS);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should downgrade PREMIUM to USER successfully")
    void upgradeAccount_PremiumToUser_Success() {
        // Arrange
        testUser.setRole(Role.PREMIUM);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.USER);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should downgrade BUSINESS to PREMIUM successfully")
    void upgradeAccount_BusinessToPremium_Success() {
        // Arrange
        testUser.setRole(Role.BUSINESS);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.PREMIUM);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should downgrade BUSINESS to USER successfully")
    void upgradeAccount_BusinessToUser_Success() {
        // Arrange
        testUser.setRole(Role.BUSINESS);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(UserResponse.builder().build());

        // Act
        userService.upgradeUserAccount(testUserId, Role.USER);

        // Assert
        assertThat(testUser.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when upgrading to same role")
    void upgradeAccount_SameRole_ThrowsException() {
        // Arrange
        testUser.setRole(Role.PREMIUM);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.upgradeUserAccount(testUserId, Role.PREMIUM))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already has role");
    }

    @Test
    @DisplayName("Should throw exception when trying to change ADMIN role")
    void upgradeAccount_AdminRole_ThrowsException() {
        // Arrange
        testUser.setRole(Role.ADMIN);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.upgradeUserAccount(testUserId, Role.PREMIUM))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot change ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when trying to change MAINTAINER role")
    void upgradeAccount_MaintainerRole_ThrowsException() {
        // Arrange
        testUser.setRole(Role.ADMIN);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.upgradeUserAccount(testUserId, Role.USER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot change");
    }

    @Test
    @DisplayName("Should throw exception when trying to upgrade to ADMIN")
    void upgradeAccount_ToAdmin_ThrowsException() {
        // Arrange
        testUser.setRole(Role.USER);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.upgradeUserAccount(testUserId, Role.ADMIN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    @DisplayName("Should throw exception when user not found during upgrade")
    void upgradeAccount_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.upgradeUserAccount(testUserId, Role.PREMIUM))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ==================== deleteUser Tests ====================

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() {
        // Arrange
        when(userRepository.existsById(testUserId)).thenReturn(true);

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository).deleteById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void deleteUser_NotFound() {
        // Arrange
        when(userRepository.existsById(testUserId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(testUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non trovato");
    }
}
