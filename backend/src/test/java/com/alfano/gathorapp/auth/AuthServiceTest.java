package com.alfano.gathorapp.auth;

import com.alfano.gathorapp.auth.dto.AuthResponse;
import com.alfano.gathorapp.auth.dto.LoginRequest;
import com.alfano.gathorapp.auth.dto.RegisterRequest;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 *
 * Tests cover:
 * - User registration with validation
 * - User login with credential verification
 * - Token generation and refresh
 * - Logout functionality
 * - Error handling for authentication failures
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User user;
    private String email;
    private String password;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@example.com";
        password = "password123";
        encodedPassword = "encodedPassword123";

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email(email)
                .passwordHash(encodedPassword)
                .role(Role.USER)
                .build();
    }

    @Test
    void testRegister_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email(email)
                .password(password)
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("accessToken", response.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(password);
    }

    @Test
    void testRegister_EmailAlreadyExists_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email(email)
                .password(password)
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("Email already in use"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(passwordEncoder, times(1)).matches(password, encodedPassword);
    }

    @Test
    void testLogin_UserNotFound_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        assertTrue(exception.getMessage().contains("Invalid credentials"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLogin_InvalidPassword_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", encodedPassword)).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        assertTrue(exception.getMessage().contains("Invalid credentials"));
    }

    @Test
    void testRefreshToken_Success() {
        // Given
        String refreshTokenString = "validRefreshToken";
        String tokenHash = "hashedToken";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(System.currentTimeMillis() / 1000 + (7 * 24 * 60 * 60)); // 7 days in epoch seconds
        refreshToken.setRevoked(false);

        when(jwtTokenProvider.validateToken(refreshTokenString)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshTokenString)).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        AuthResponse response = authService.refreshToken(refreshTokenString);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).revokeByTokenHash(anyString());
    }

    @Test
    void testRefreshToken_InvalidToken_ThrowsException() {
        // Given
        String refreshTokenString = "invalidRefreshToken";

        when(jwtTokenProvider.validateToken(refreshTokenString)).thenReturn(false);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(refreshTokenString);
        });

        assertTrue(exception.getMessage().contains("Invalid refresh token"));
    }

    @Test
    void testRefreshToken_TokenNotFoundInDatabase_ThrowsException() {
        // Given
        String refreshTokenString = "validRefreshToken";

        when(jwtTokenProvider.validateToken(refreshTokenString)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshTokenString)).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.empty());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(refreshTokenString);
        });

        assertTrue(exception.getMessage().contains("Refresh token not found or revoked"));
    }

    @Test
    void testRefreshToken_TokenExpired_ThrowsException() {
        // Given
        String refreshTokenString = "expiredRefreshToken";
        String tokenHash = "hashedToken";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(System.currentTimeMillis() / 1000 - (24 * 60 * 60)); // Expired 1 day ago
        refreshToken.setRevoked(false);

        when(jwtTokenProvider.validateToken(refreshTokenString)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshTokenString)).thenReturn(userId);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(refreshToken));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(refreshTokenString);
        });

        assertTrue(exception.getMessage().contains("Refresh token expired"));
    }

    @Test
    void testLogout_Success() {
        // Given
        String refreshTokenString = "validRefreshToken";

        doNothing().when(refreshTokenRepository).revokeByTokenHash(anyString());

        // When
        authService.logout(refreshTokenString);

        // Then
        verify(refreshTokenRepository, times(1)).revokeByTokenHash(anyString());
    }

    @Test
    void testGenerateAuthResponse_ContainsUserInfo() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email(email)
                .password(password)
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(userId, email, "USER"))
                .thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals(email, response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getName());
        assertEquals("USER", response.getUser().getRole());
    }
}
