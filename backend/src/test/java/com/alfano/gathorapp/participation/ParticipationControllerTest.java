package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
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
 * Unit tests for ParticipationController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationController Tests")
class ParticipationControllerTest {

    @Mock
    private ParticipationService participationService;

    @InjectMocks
    private ParticipationController participationController;

    private UUID userId;
    private UUID outingId;
    private UUID participationId;
    private SecurityUser securityUser;
    private ParticipationResponse participationResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        participationId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        participationResponse = ParticipationResponse.builder()
                .id(participationId)
                .status(ParticipationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .user(ParticipationResponse.UserInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .email("test@example.com")
                        .build())
                .outing(ParticipationResponse.OutingInfo.builder()
                        .id(outingId)
                        .title("Test Outing")
                        .build())
                .build();
    }

    @Test
    @DisplayName("GET /api/participations/outing/{outingId} - Should return participations list")
    void getParticipationsByOuting_ReturnsParticipationsList() {
        // Given
        List<ParticipationResponse> mockParticipations = new ArrayList<>();
        mockParticipations.add(participationResponse);
        when(participationService.getParticipationsByOuting(outingId)).thenReturn(mockParticipations);

        // When
        ResponseEntity<List<ParticipationResponse>> response =
                participationController.getParticipationsByOuting(outingId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(participationId, response.getBody().get(0).getId());
        verify(participationService, times(1)).getParticipationsByOuting(outingId);
    }

    @Test
    @DisplayName("GET /api/participations/outing/{outingId} - Should return empty list when no participations")
    void getParticipationsByOuting_NoParticipations_ReturnsEmptyList() {
        // Given
        when(participationService.getParticipationsByOuting(outingId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<ParticipationResponse>> response =
                participationController.getParticipationsByOuting(outingId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(participationService, times(1)).getParticipationsByOuting(outingId);
    }

    @Test
    @DisplayName("GET /api/participations/my - Should return user's participations")
    void getMyParticipations_ReturnsUserParticipations() {
        // Given
        List<ParticipationResponse> mockParticipations = new ArrayList<>();
        mockParticipations.add(participationResponse);
        when(participationService.getParticipationsByUser(userId)).thenReturn(mockParticipations);

        // When
        ResponseEntity<List<ParticipationResponse>> response =
                participationController.getMyParticipations(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(participationService, times(1)).getParticipationsByUser(userId);
    }

    @Test
    @DisplayName("GET /api/participations/my - Should return empty list when user has no participations")
    void getMyParticipations_NoParticipations_ReturnsEmptyList() {
        // Given
        when(participationService.getParticipationsByUser(userId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<ParticipationResponse>> response =
                participationController.getMyParticipations(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(participationService, times(1)).getParticipationsByUser(userId);
    }

    @Test
    @DisplayName("POST /api/participations/outing/{outingId} - Should join outing successfully")
    void joinOuting_ValidRequest_JoinsOuting() {
        // Given
        when(participationService.joinOuting(outingId, userId)).thenReturn(participationResponse);

        // When
        ResponseEntity<ParticipationResponse> response =
                participationController.joinOuting(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(participationId, response.getBody().getId());
        assertEquals(ParticipationStatus.APPROVED, response.getBody().getStatus());
        verify(participationService, times(1)).joinOuting(outingId, userId);
    }

    @Test
    @DisplayName("POST /api/participations/outing/{outingId} - Should join different outings")
    void joinOuting_DifferentOutings_JoinsEachSuccessfully() {
        // Given
        UUID outingId2 = UUID.randomUUID();
        ParticipationResponse participation2 = ParticipationResponse.builder()
                .id(UUID.randomUUID())
                .status(ParticipationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .user(ParticipationResponse.UserInfo.builder()
                        .id(userId)
                        .name("Test User")
                        .email("test@example.com")
                        .build())
                .outing(ParticipationResponse.OutingInfo.builder()
                        .id(outingId2)
                        .title("Another Outing")
                        .build())
                .build();

        when(participationService.joinOuting(outingId, userId)).thenReturn(participationResponse);
        when(participationService.joinOuting(outingId2, userId)).thenReturn(participation2);

        // When
        ResponseEntity<ParticipationResponse> response1 =
                participationController.joinOuting(outingId, securityUser);
        ResponseEntity<ParticipationResponse> response2 =
                participationController.joinOuting(outingId2, securityUser);

        // Then
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertEquals(outingId, response1.getBody().getOuting().getId());
        assertEquals(outingId2, response2.getBody().getOuting().getId());
        verify(participationService, times(1)).joinOuting(outingId, userId);
        verify(participationService, times(1)).joinOuting(outingId2, userId);
    }

    @Test
    @DisplayName("PUT /api/participations/{id}/approve - Should approve participation")
    void approveParticipation_ValidRequest_ApprovesParticipation() {
        // Given
        when(participationService.approveParticipation(participationId, userId))
                .thenReturn(participationResponse);

        // When
        ResponseEntity<ParticipationResponse> response =
                participationController.approveParticipation(participationId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participationId, response.getBody().getId());
        assertEquals(ParticipationStatus.APPROVED, response.getBody().getStatus());
        verify(participationService, times(1)).approveParticipation(participationId, userId);
    }

    @Test
    @DisplayName("PUT /api/participations/{id}/approve - Should approve different participations")
    void approveParticipation_DifferentParticipations_ApprovesEach() {
        // Given
        UUID participationId2 = UUID.randomUUID();
        ParticipationResponse participation2 = ParticipationResponse.builder()
                .id(participationId2)
                .status(ParticipationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();

        when(participationService.approveParticipation(participationId, userId))
                .thenReturn(participationResponse);
        when(participationService.approveParticipation(participationId2, userId))
                .thenReturn(participation2);

        // When
        ResponseEntity<ParticipationResponse> response1 =
                participationController.approveParticipation(participationId, securityUser);
        ResponseEntity<ParticipationResponse> response2 =
                participationController.approveParticipation(participationId2, securityUser);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(participationId, response1.getBody().getId());
        assertEquals(participationId2, response2.getBody().getId());
        verify(participationService, times(1)).approveParticipation(participationId, userId);
        verify(participationService, times(1)).approveParticipation(participationId2, userId);
    }

    @Test
    @DisplayName("PUT /api/participations/{id}/reject - Should reject participation")
    void rejectParticipation_ValidRequest_RejectsParticipation() {
        // Given
        ParticipationResponse rejectedResponse = ParticipationResponse.builder()
                .id(participationId)
                .status(ParticipationStatus.REJECTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(participationService.rejectParticipation(participationId, userId))
                .thenReturn(rejectedResponse);

        // When
        ResponseEntity<ParticipationResponse> response =
                participationController.rejectParticipation(participationId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participationId, response.getBody().getId());
        assertEquals(ParticipationStatus.REJECTED, response.getBody().getStatus());
        verify(participationService, times(1)).rejectParticipation(participationId, userId);
    }

    @Test
    @DisplayName("DELETE /api/participations/{id} - Should leave outing successfully")
    void leaveOuting_ValidRequest_LeavesOuting() {
        // Given
        doNothing().when(participationService).leaveOuting(participationId, userId);

        // When
        ResponseEntity<Void> response =
                participationController.leaveOuting(participationId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(participationService, times(1)).leaveOuting(participationId, userId);
    }

    @Test
    @DisplayName("DELETE /api/participations/{id} - Should leave different participations")
    void leaveOuting_DifferentParticipations_LeavesEach() {
        // Given
        UUID participationId2 = UUID.randomUUID();
        doNothing().when(participationService).leaveOuting(participationId, userId);
        doNothing().when(participationService).leaveOuting(participationId2, userId);

        // When
        ResponseEntity<Void> response1 =
                participationController.leaveOuting(participationId, securityUser);
        ResponseEntity<Void> response2 =
                participationController.leaveOuting(participationId2, securityUser);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response2.getStatusCode());
        verify(participationService, times(1)).leaveOuting(participationId, userId);
        verify(participationService, times(1)).leaveOuting(participationId2, userId);
    }

    @Test
    @DisplayName("GET /api/participations/outing/{outingId} - Should handle multiple participations")
    void getParticipationsByOuting_MultipleParticipations_ReturnsAll() {
        // Given
        List<ParticipationResponse> mockParticipations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockParticipations.add(ParticipationResponse.builder()
                    .id(UUID.randomUUID())
                    .status(ParticipationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        when(participationService.getParticipationsByOuting(outingId)).thenReturn(mockParticipations);

        // When
        ResponseEntity<List<ParticipationResponse>> response =
                participationController.getParticipationsByOuting(outingId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().size());
        verify(participationService, times(1)).getParticipationsByOuting(outingId);
    }
}
