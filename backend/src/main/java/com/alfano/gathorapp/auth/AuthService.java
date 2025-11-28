package com.alfano.gathorapp.auth;

import com.alfano.gathorapp.auth.dto.AuthResponse;
import com.alfano.gathorapp.auth.dto.LoginRequest;
import com.alfano.gathorapp.auth.dto.RegisterRequest;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service handling authentication operations.
 * Manages user registration, login, token generation, and refresh token
 * rotation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Authenticate user and generate tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if user is banned
        if (user.isBanned()) {
            log.warn("Banned user attempted login: {}", request.getEmail());
            throw new RuntimeException("Account has been banned. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        log.info("User logged in successfully: {}", user.getId());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        log.debug("Refreshing access token");

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user ID from token
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshTokenString);

        // Check if token exists in database and is valid
        String tokenHash = hashToken(refreshTokenString);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or revoked"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token expired");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke old refresh token (token rotation)
        refreshTokenRepository.revokeByTokenHash(tokenHash);

        // Generate new tokens
        log.info("Tokens refreshed for user: {}", user.getId());
        return generateAuthResponse(user);
    }

    /**
     * Logout user by revoking refresh token.
     */
    @Transactional
    public void logout(String refreshTokenString) {
        String tokenHash = hashToken(refreshTokenString);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
        log.info("User logged out, refresh token revoked");
    }

    /**
     * Generate authentication response with tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());

        // Generate refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Save refresh token to database
        saveRefreshToken(user, refreshToken);

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    /**
     * Save refresh token to database.
     */
    private void saveRefreshToken(User user, String token) {
        Long expiresAt = jwtTokenProvider.getExpirationFromToken(token);
        String tokenHash = hashToken(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Hash token using SHA-256 for storage.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
