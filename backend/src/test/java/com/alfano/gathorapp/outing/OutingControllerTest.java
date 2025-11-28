package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutingController.
 * Uses Mockito to test controller logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutingController Tests")
class OutingControllerTest {

    @Mock
    private OutingService outingService;

    @InjectMocks
    private OutingController outingController;

    private UUID userId;
    private UUID outingId;
    private UUID eventId;
    private SecurityUser securityUser;
    private OutingResponse outingResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        outingId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        securityUser = new SecurityUser(user);

        outingResponse = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .description("Test Description")
                .location("Test Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .currentParticipants(0)
                .isParticipant(false)
                .isFull(false)
                .build();
    }

    @Test
    @DisplayName("GET /api/outings - Should return all outings")
    void getAllOutings_ReturnsOutingList() {
        // Given
        List<OutingResponse> mockOutings = new ArrayList<>();
        mockOutings.add(outingResponse);

        when(outingService.getAllOutings()).thenReturn(mockOutings);

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getAllOutings();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Test Outing", response.getBody().get(0).getTitle());
        verify(outingService, times(1)).getAllOutings();
    }

    @Test
    @DisplayName("GET /api/outings/upcoming - Should return upcoming outings")
    void getUpcomingOutings_ReturnsUpcomingOutingList() {
        // Given
        List<OutingResponse> mockOutings = new ArrayList<>();
        mockOutings.add(outingResponse);

        when(outingService.getUpcomingOutings()).thenReturn(mockOutings);

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getUpcomingOutings();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(outingService, times(1)).getUpcomingOutings();
    }

    @Test
    @DisplayName("GET /api/outings/{id} - Should return outing by ID with user context")
    void getOutingById_WithUserContext_ReturnsOuting() {
        // Given
        when(outingService.getOutingById(eq(outingId), eq(userId))).thenReturn(outingResponse);

        // When
        ResponseEntity<OutingResponse> response = outingController.getOutingById(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(outingId, response.getBody().getId());
        assertEquals("Test Outing", response.getBody().getTitle());
        verify(outingService, times(1)).getOutingById(eq(outingId), eq(userId));
    }

    @Test
    @DisplayName("GET /api/outings/{id} - Should handle null user context")
    void getOutingById_NullUserContext_ReturnsOuting() {
        // Given
        when(outingService.getOutingById(eq(outingId), isNull())).thenReturn(outingResponse);

        // When
        ResponseEntity<OutingResponse> response = outingController.getOutingById(outingId, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outingService, times(1)).getOutingById(eq(outingId), isNull());
    }

    @Test
    @DisplayName("GET /api/outings/my - Should return user's organized outings")
    void getMyOutings_ReturnsUserOutings() {
        // Given
        List<OutingResponse> mockOutings = new ArrayList<>();
        mockOutings.add(outingResponse);

        when(outingService.getOutingsByOrganizer(userId)).thenReturn(mockOutings);

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getMyOutings(securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(outingService, times(1)).getOutingsByOrganizer(userId);
    }

    @Test
    @DisplayName("GET /api/outings/event/{eventId} - Should return outings for event")
    void getOutingsByEvent_ReturnsOutingsForEvent() {
        // Given
        List<OutingResponse> mockOutings = new ArrayList<>();
        mockOutings.add(outingResponse);

        when(outingService.getOutingsByEvent(eventId)).thenReturn(mockOutings);

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getOutingsByEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(outingService, times(1)).getOutingsByEvent(eventId);
    }

    @Test
    @DisplayName("POST /api/outings - Should create outing successfully")
    void createOuting_Success_ReturnsCreatedOuting() {
        // Given
        CreateOutingRequest request = CreateOutingRequest.builder()
                .title("New Outing")
                .description("New Description")
                .location("New Location")
                .latitude(40.3515)
                .longitude(18.1750)
                .outingDate(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .eventId(eventId)
                .build();

        when(outingService.createOuting(any(CreateOutingRequest.class), eq(userId)))
                .thenReturn(outingResponse);

        // When
        ResponseEntity<OutingResponse> response = outingController.createOuting(request, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(outingService, times(1)).createOuting(any(CreateOutingRequest.class), eq(userId));
    }

    @Test
    @DisplayName("DELETE /api/outings/{id} - Should delete outing successfully")
    void deleteOuting_Success_ReturnsNoContent() {
        // Given
        doNothing().when(outingService).deleteOuting(outingId, userId);

        // When
        ResponseEntity<Void> response = outingController.deleteOuting(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(outingService, times(1)).deleteOuting(outingId, userId);
    }

    @Test
    @DisplayName("POST /api/outings/{id}/join - Should join outing successfully")
    void joinOuting_Success_ReturnsUpdatedOuting() {
        // Given
        OutingResponse joinedResponse = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .currentParticipants(1)
                .isParticipant(true)
                .build();

        when(outingService.joinOuting(outingId, userId)).thenReturn(joinedResponse);

        // When
        ResponseEntity<OutingResponse> response = outingController.joinOuting(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getIsParticipant());
        assertEquals(1, response.getBody().getCurrentParticipants());
        verify(outingService, times(1)).joinOuting(outingId, userId);
    }

    @Test
    @DisplayName("POST /api/outings/{id}/leave - Should leave outing successfully")
    void leaveOuting_Success_ReturnsUpdatedOuting() {
        // Given
        OutingResponse leftResponse = OutingResponse.builder()
                .id(outingId)
                .title("Test Outing")
                .currentParticipants(0)
                .isParticipant(false)
                .build();

        when(outingService.leaveOuting(outingId, userId)).thenReturn(leftResponse);

        // When
        ResponseEntity<OutingResponse> response = outingController.leaveOuting(outingId, securityUser);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getIsParticipant());
        assertEquals(0, response.getBody().getCurrentParticipants());
        verify(outingService, times(1)).leaveOuting(outingId, userId);
    }

    @Test
    @DisplayName("GET /api/outings - Should return empty list when no outings exist")
    void getAllOutings_EmptyList_ReturnsEmptyList() {
        // Given
        when(outingService.getAllOutings()).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getAllOutings();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /api/outings/event/{eventId} - Should return empty list when event has no outings")
    void getOutingsByEvent_EmptyList_ReturnsEmptyList() {
        // Given
        when(outingService.getOutingsByEvent(eventId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<OutingResponse>> response = outingController.getOutingsByEvent(eventId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
