package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParticipationMapper.
 */
@DisplayName("ParticipationMapper Tests")
class ParticipationMapperTest {

    private ParticipationMapper participationMapper;

    private UUID userId;
    private UUID outingId;
    private UUID participationId;
    private User user;
    private Outing outing;
    private Participation participation;

    @BeforeEach
    void setUp() {
        participationMapper = new ParticipationMapper();

        userId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        participationId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        outing = Outing.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Description")
                .outingDate(LocalDateTime.of(2024, 12, 31, 20, 0))
                .maxParticipants(10)
                .build();

        participation = Participation.builder()
                .id(participationId)
                .user(user)
                .outing(outing)
                .status(ParticipationStatus.APPROVED)
                .createdAt(LocalDateTime.of(2024, 12, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("toResponse - Should map participation to response DTO correctly")
    void toResponse_ValidParticipation_MapsAllFieldsCorrectly() {
        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(participationId, response.getId());
        assertEquals(ParticipationStatus.APPROVED, response.getStatus());
        assertEquals(LocalDateTime.of(2024, 12, 1, 10, 0), response.getCreatedAt());

        // Verify user info
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals("Test User", response.getUser().getName());
        assertEquals("test@example.com", response.getUser().getEmail());

        // Verify outing info
        assertNotNull(response.getOuting());
        assertEquals(outingId, response.getOuting().getId());
        assertEquals("Test Outing", response.getOuting().getTitle());
        assertEquals(LocalDateTime.of(2024, 12, 31, 20, 0), response.getOuting().getOutingDate());
        assertEquals(10, response.getOuting().getMaxParticipants());
    }

    @Test
    @DisplayName("toResponse - Should map participation with PENDING status")
    void toResponse_PendingStatus_MapsPendingCorrectly() {
        // Given
        participation.setStatus(ParticipationStatus.PENDING);

        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(ParticipationStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("toResponse - Should map participation with REJECTED status")
    void toResponse_RejectedStatus_MapsRejectedCorrectly() {
        // Given
        participation.setStatus(ParticipationStatus.REJECTED);

        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(ParticipationStatus.REJECTED, response.getStatus());
    }

    @Test
    @DisplayName("toResponse - Should map different users correctly")
    void toResponse_DifferentUsers_MapsDifferentUsersCorrectly() {
        // Given
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .name("Another User")
                .email("another@example.com")
                .passwordHash("pass")
                .role(Role.USER)
                .build();

        participation.setUser(user2);

        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(user2.getId(), response.getUser().getId());
        assertEquals("Another User", response.getUser().getName());
        assertEquals("another@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("toResponse - Should map different outings correctly")
    void toResponse_DifferentOutings_MapsDifferentOutingsCorrectly() {
        // Given
        Outing outing2 = Outing.builder()
                .id(UUID.randomUUID())
                .title("Another Outing")
                .description("Another Description")
                .outingDate(LocalDateTime.of(2025, 1, 15, 18, 30))
                .maxParticipants(20)
                .build();

        participation.setOuting(outing2);

        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(outing2.getId(), response.getOuting().getId());
        assertEquals("Another Outing", response.getOuting().getTitle());
        assertEquals(LocalDateTime.of(2025, 1, 15, 18, 30), response.getOuting().getOutingDate());
        assertEquals(20, response.getOuting().getMaxParticipants());
    }

    @Test
    @DisplayName("toResponse - Should preserve all timestamps correctly")
    void toResponse_DifferentTimestamps_MapsTimestampsCorrectly() {
        // Given
        LocalDateTime customCreatedAt = LocalDateTime.of(2023, 6, 15, 14, 30);
        participation.setCreatedAt(customCreatedAt);

        // When
        ParticipationResponse response = participationMapper.toResponse(participation);

        // Then
        assertNotNull(response);
        assertEquals(customCreatedAt, response.getCreatedAt());
    }

    @Test
    @DisplayName("toResponse - Should map multiple participations independently")
    void toResponse_MultipleParticipations_MapsEachIndependently() {
        // Given
        Participation participation2 = Participation.builder()
                .id(UUID.randomUUID())
                .user(User.builder()
                        .id(UUID.randomUUID())
                        .name("User 2")
                        .email("user2@example.com")
                        .passwordHash("pass")
                        .role(Role.USER)
                        .build())
                .outing(Outing.builder()
                        .id(UUID.randomUUID())
                        .title("Outing 2")
                        .outingDate(LocalDateTime.of(2025, 2, 20, 16, 0))
                        .maxParticipants(5)
                        .build())
                .status(ParticipationStatus.PENDING)
                .createdAt(LocalDateTime.of(2024, 11, 20, 9, 0))
                .build();

        // When
        ParticipationResponse response1 = participationMapper.toResponse(participation);
        ParticipationResponse response2 = participationMapper.toResponse(participation2);

        // Then
        assertNotNull(response1);
        assertNotNull(response2);

        // Verify they are independent
        assertNotEquals(response1.getId(), response2.getId());
        assertNotEquals(response1.getUser().getId(), response2.getUser().getId());
        assertNotEquals(response1.getOuting().getId(), response2.getOuting().getId());
        assertEquals(ParticipationStatus.APPROVED, response1.getStatus());
        assertEquals(ParticipationStatus.PENDING, response2.getStatus());
    }
}
