package com.alfano.gathorapp.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for JwtTokenProvider.
 * Tests cover token generation, validation, parsing, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private UUID testUserId;
    private String testEmail;
    private String testRole;

    // Test secret key - must be at least 256 bits (32 characters)
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final long ACCESS_TOKEN_VALIDITY = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_VALIDITY = 2592000000L; // 30 days

    @BeforeEach
    void setUp() {
        // Initialize JwtTokenProvider with test configuration
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_VALIDITY, REFRESH_TOKEN_VALIDITY);

        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testRole = "USER";
    }

    // ==================== generateAccessToken Tests ====================

    @Test
    @DisplayName("Should generate valid access token with all claims")
    void generateAccessToken_Success() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature

        // Verify token is valid
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate access token with correct user ID")
    void generateAccessToken_CorrectUserId() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // Then
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Should generate access token with correct email")
    void generateAccessToken_CorrectEmail() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // Then
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Should generate access token with expiration time")
    void generateAccessToken_HasExpiration() {
        // Given
        long beforeGeneration = System.currentTimeMillis() / 1000; // Convert to seconds

        // When
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // Then
        Long expiration = jwtTokenProvider.getExpirationFromToken(token);
        assertThat(expiration).isNotNull();
        assertThat(expiration).isGreaterThan(beforeGeneration);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateAccessToken_DifferentUsersGetDifferentTokens() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        // When
        String token1 = jwtTokenProvider.generateAccessToken(userId1, "user1@example.com", "USER");
        String token2 = jwtTokenProvider.generateAccessToken(userId2, "user2@example.com", "USER");

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    // ==================== generateRefreshToken Tests ====================

    @Test
    @DisplayName("Should generate valid refresh token")
    void generateRefreshToken_Success() {
        // When
        String token = jwtTokenProvider.generateRefreshToken(testUserId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate refresh token with correct user ID")
    void generateRefreshToken_CorrectUserId() {
        // When
        String token = jwtTokenProvider.generateRefreshToken(testUserId);

        // Then
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Should generate refresh token with longer expiration than access token")
    void generateRefreshToken_LongerExpiration() {
        // When
        String accessToken = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUserId);

        // Then
        Long accessExpiration = jwtTokenProvider.getExpirationFromToken(accessToken);
        Long refreshExpiration = jwtTokenProvider.getExpirationFromToken(refreshToken);

        assertThat(refreshExpiration).isGreaterThan(accessExpiration);
    }

    // ==================== getUserIdFromToken Tests ====================

    @Test
    @DisplayName("Should extract user ID from valid token")
    void getUserIdFromToken_Success() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Should throw exception for invalid token when extracting user ID")
    void getUserIdFromToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== getEmailFromToken Tests ====================

    @Test
    @DisplayName("Should extract email from valid token")
    void getEmailFromToken_Success() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Should throw exception for invalid token when extracting email")
    void getEmailFromToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.getEmailFromToken(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== getExpirationFromToken Tests ====================

    @Test
    @DisplayName("Should extract expiration from valid token")
    void getExpirationFromToken_Success() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        Long expiration = jwtTokenProvider.getExpirationFromToken(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isGreaterThan(System.currentTimeMillis() / 1000); // Compare with seconds
    }

    @Test
    @DisplayName("Should throw exception for invalid token when extracting expiration")
    void getExpirationFromToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.getExpirationFromToken(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== validateToken Tests ====================

    @Test
    @DisplayName("Should validate correct access token")
    void validateToken_ValidAccessToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate correct refresh token")
    void validateToken_ValidRefreshToken() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUserId);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_MalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void validateToken_EmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null token")
    void validateToken_NullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void validateToken_InvalidSignature() {
        // Given - Create a token with a different secret
        String differentSecret = "different-secret-key-that-is-at-least-256-bits-long-for-testing";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithDifferentSignature = Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", testEmail)
                .claim("role", testRole)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== Token Expiration Tests ====================

    @Test
    @DisplayName("Should reject expired token")
    void validateToken_ExpiredToken() {
        // Given - Create an expired token
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", testEmail)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .signWith(key)
                .compact();

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle user ID with special characters in email")
    void generateAccessToken_SpecialCharactersInEmail() {
        // Given
        String emailWithSpecialChars = "test+tag@example.co.uk";

        // When
        String token = jwtTokenProvider.generateAccessToken(testUserId, emailWithSpecialChars, testRole);

        // Then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(emailWithSpecialChars);
    }

    @Test
    @DisplayName("Should handle different role types")
    void generateAccessToken_DifferentRoles() {
        // Given
        String[] roles = {"USER", "PREMIUM", "BUSINESS", "ADMIN"};

        for (String role : roles) {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUserId, testEmail, role);

            // Then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }
    }

    @Test
    @DisplayName("Should generate multiple valid tokens")
    void generateAccessToken_MultipleTokensInQuickSuccession() throws InterruptedException {
        // When
        String token1 = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);
        Thread.sleep(1); // Ensure different timestamp
        String token2 = jwtTokenProvider.generateAccessToken(testUserId, testEmail, testRole);

        // Then - Both tokens should be valid, may or may not be equal depending on timestamp precision
        assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
        assertThat(jwtTokenProvider.validateToken(token2)).isTrue();

        // Verify both contain correct user ID
        assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(testUserId);
        assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(testUserId);
    }
}
