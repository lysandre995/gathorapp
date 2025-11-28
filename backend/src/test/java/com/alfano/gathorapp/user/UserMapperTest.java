package com.alfano.gathorapp.user;

import com.alfano.gathorapp.user.dto.CreateUserRequest;
import com.alfano.gathorapp.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserMapper.
 */
@DisplayName("UserMapper Tests")
class UserMapperTest {

    private UserMapper userMapper;

    private UUID userId;
    private User user;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .role(Role.USER)
                .createdAt(LocalDateTime.of(2024, 12, 1, 10, 0))
                .build();

        createUserRequest = CreateUserRequest.builder()
                .name("New User")
                .email("new@example.com")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("toResponse - Should map user entity to response DTO correctly")
    void toResponse_ValidUser_MapsAllFieldsCorrectly() {
        // When
        UserResponse response = userMapper.toResponse(user);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertEquals(LocalDateTime.of(2024, 12, 1, 10, 0), response.getCreatedAt());
    }

    @Test
    @DisplayName("toResponse - Should not expose password hash")
    void toResponse_ValidUser_DoesNotExposePasswordHash() {
        // When
        UserResponse response = userMapper.toResponse(user);

        // Then
        assertNotNull(response);
        // UserResponse DTO should not have password field at all
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("toResponse - Should map users with different roles correctly")
    void toResponse_DifferentRoles_MapsDifferentRolesCorrectly() {
        // Given
        User basicUser = User.builder()
                .id(UUID.randomUUID())
                .name("Basic User")
                .email("basic@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        User premiumUser = User.builder()
                .id(UUID.randomUUID())
                .name("Premium User")
                .email("premium@example.com")
                .passwordHash("hash")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.now())
                .build();

        User businessUser = User.builder()
                .id(UUID.randomUUID())
                .name("Business User")
                .email("business@example.com")
                .passwordHash("hash")
                .role(Role.BUSINESS)
                .createdAt(LocalDateTime.now())
                .build();

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@example.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        UserResponse basicResponse = userMapper.toResponse(basicUser);
        UserResponse premiumResponse = userMapper.toResponse(premiumUser);
        UserResponse businessResponse = userMapper.toResponse(businessUser);
        UserResponse adminResponse = userMapper.toResponse(adminUser);

        // Then
        assertEquals(Role.USER, basicResponse.getRole());
        assertEquals(Role.PREMIUM, premiumResponse.getRole());
        assertEquals(Role.BUSINESS, businessResponse.getRole());
        assertEquals(Role.ADMIN, adminResponse.getRole());
    }

    @Test
    @DisplayName("toResponse - Should map different users independently")
    void toResponse_MultipleUsers_MapsEachIndependently() {
        // Given
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .name("User One")
                .email("one@example.com")
                .passwordHash("hash1")
                .role(Role.USER)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .name("User Two")
                .email("two@example.com")
                .passwordHash("hash2")
                .role(Role.PREMIUM)
                .createdAt(LocalDateTime.of(2024, 6, 15, 14, 30))
                .build();

        // When
        UserResponse response1 = userMapper.toResponse(user1);
        UserResponse response2 = userMapper.toResponse(user2);

        // Then
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotEquals(response1.getId(), response2.getId());
        assertEquals("User One", response1.getName());
        assertEquals("User Two", response2.getName());
        assertEquals(Role.USER, response1.getRole());
        assertEquals(Role.PREMIUM, response2.getRole());
    }

    @Test
    @DisplayName("toResponse - Should map different timestamps correctly")
    void toResponse_DifferentTimestamps_MapsTimestampsCorrectly() {
        // Given
        LocalDateTime timestamp1 = LocalDateTime.of(2023, 1, 1, 10, 0);
        LocalDateTime timestamp2 = LocalDateTime.of(2024, 12, 31, 23, 59);

        user.setCreatedAt(timestamp1);
        UserResponse response1 = userMapper.toResponse(user);

        user.setCreatedAt(timestamp2);
        UserResponse response2 = userMapper.toResponse(user);

        // Then
        assertEquals(timestamp1, response1.getCreatedAt());
        assertEquals(timestamp2, response2.getCreatedAt());
    }

    @Test
    @DisplayName("toEntity - Should map CreateUserRequest to User entity correctly")
    void toEntity_ValidRequest_MapsAllFieldsCorrectly() {
        // When
        User entity = userMapper.toEntity(createUserRequest);

        // Then
        assertNotNull(entity);
        assertEquals("New User", entity.getName());
        assertEquals("new@example.com", entity.getEmail());
        assertEquals(Role.USER, entity.getRole());
    }

    @Test
    @DisplayName("toEntity - Should default to USER role when role is null")
    void toEntity_NullRole_DefaultsToUserRole() {
        // Given
        CreateUserRequest requestWithoutRole = CreateUserRequest.builder()
                .name("No Role User")
                .email("norole@example.com")
                .role(null)
                .build();

        // When
        User entity = userMapper.toEntity(requestWithoutRole);

        // Then
        assertNotNull(entity);
        assertEquals("No Role User", entity.getName());
        assertEquals("norole@example.com", entity.getEmail());
        assertEquals(Role.USER, entity.getRole()); // Should default to USER
    }

    @Test
    @DisplayName("toEntity - Should map requests with different roles correctly")
    void toEntity_DifferentRoles_MapsDifferentRolesCorrectly() {
        // Given
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("Basic User")
                .email("basic@example.com")
                .role(Role.USER)
                .build();

        CreateUserRequest premiumRequest = CreateUserRequest.builder()
                .name("Premium User")
                .email("premium@example.com")
                .role(Role.PREMIUM)
                .build();

        CreateUserRequest businessRequest = CreateUserRequest.builder()
                .name("Business User")
                .email("business@example.com")
                .role(Role.BUSINESS)
                .build();

        CreateUserRequest adminRequest = CreateUserRequest.builder()
                .name("Admin User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        // When
        User basicEntity = userMapper.toEntity(userRequest);
        User premiumEntity = userMapper.toEntity(premiumRequest);
        User businessEntity = userMapper.toEntity(businessRequest);
        User adminEntity = userMapper.toEntity(adminRequest);

        // Then
        assertEquals(Role.USER, basicEntity.getRole());
        assertEquals(Role.PREMIUM, premiumEntity.getRole());
        assertEquals(Role.BUSINESS, businessEntity.getRole());
        assertEquals(Role.ADMIN, adminEntity.getRole());
    }

    @Test
    @DisplayName("toEntity - Should map different requests independently")
    void toEntity_MultipleRequests_MapsEachIndependently() {
        // Given
        CreateUserRequest request1 = CreateUserRequest.builder()
                .name("User One")
                .email("one@example.com")
                .role(Role.USER)
                .build();

        CreateUserRequest request2 = CreateUserRequest.builder()
                .name("User Two")
                .email("two@example.com")
                .role(Role.PREMIUM)
                .build();

        // When
        User entity1 = userMapper.toEntity(request1);
        User entity2 = userMapper.toEntity(request2);

        // Then
        assertNotNull(entity1);
        assertNotNull(entity2);
        assertEquals("User One", entity1.getName());
        assertEquals("User Two", entity2.getName());
        assertEquals("one@example.com", entity1.getEmail());
        assertEquals("two@example.com", entity2.getEmail());
        assertEquals(Role.USER, entity1.getRole());
        assertEquals(Role.PREMIUM, entity2.getRole());
    }

    @Test
    @DisplayName("toEntity - Should not set ID, password, or createdAt")
    void toEntity_ValidRequest_DoesNotSetIdPasswordOrTimestamp() {
        // When
        User entity = userMapper.toEntity(createUserRequest);

        // Then
        assertNotNull(entity);
        assertNull(entity.getId()); // ID should be null, will be generated by DB
        assertNull(entity.getPasswordHash()); // Password should be set separately
        assertNull(entity.getCreatedAt()); // CreatedAt should be null, will be set by @PrePersist
    }

    @Test
    @DisplayName("toEntity - Should handle requests with different names")
    void toEntity_DifferentNames_MapsDifferentNamesCorrectly() {
        // Given
        createUserRequest.setName("Alice");
        User entity1 = userMapper.toEntity(createUserRequest);

        createUserRequest.setName("Bob");
        User entity2 = userMapper.toEntity(createUserRequest);

        // Then
        assertEquals("Alice", entity1.getName());
        assertEquals("Bob", entity2.getName());
    }

    @Test
    @DisplayName("toEntity - Should handle requests with different emails")
    void toEntity_DifferentEmails_MapsDifferentEmailsCorrectly() {
        // Given
        createUserRequest.setEmail("alice@example.com");
        User entity1 = userMapper.toEntity(createUserRequest);

        createUserRequest.setEmail("bob@example.com");
        User entity2 = userMapper.toEntity(createUserRequest);

        // Then
        assertEquals("alice@example.com", entity1.getEmail());
        assertEquals("bob@example.com", entity2.getEmail());
    }
}
