package com.alfano.gathorapp.security;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for JwtAuthenticationFilter.
 * Tests JWT token extraction, validation, and security context setup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private String validToken;
    private String testEmail;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        validToken = "valid.jwt.token";
        testEmail = "test@example.com";

        userDetails = User.builder()
                .username(testEmail)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    // ==================== Successful Authentication Tests ====================

    @Test
    @DisplayName("Should authenticate user with valid Bearer token")
    void doFilterInternal_ValidBearerToken_SetsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();

        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getEmailFromToken(validToken);
        verify(userDetailsService).loadUserByUsername(testEmail);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set correct authorities in authentication")
    void doFilterInternal_ValidToken_SetsCorrectAuthorities() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    // ==================== No Token Tests ====================

    @Test
    @DisplayName("Should not authenticate when no Authorization header")
    void doFilterInternal_NoAuthHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when Authorization header does not start with Bearer")
    void doFilterInternal_NoBearerPrefix_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic " + validToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when Authorization header is empty")
    void doFilterInternal_EmptyAuthHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Invalid Token Tests ====================

    @Test
    @DisplayName("Should not authenticate with invalid token")
    void doFilterInternal_InvalidToken_NoAuthentication() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with expired token")
    void doFilterInternal_ExpiredToken_NoAuthentication() throws ServletException, IOException {
        // Given
        String expiredToken = "expired.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(jwtTokenProvider.validateToken(expiredToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider).validateToken(expiredToken);
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Should handle exception when validating token and continue filter chain")
    void doFilterInternal_TokenValidationThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken))
                .thenThrow(new RuntimeException("Token validation error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle exception when loading user details and continue filter chain")
    void doFilterInternal_UserDetailsLoadingThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail))
                .thenThrow(new RuntimeException("User not found"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle exception when getting email from token and continue filter chain")
    void doFilterInternal_GetEmailThrowsException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken))
                .thenThrow(new RuntimeException("Cannot extract email"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Token Format Tests ====================

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void doFilterInternal_BearerTokenWithSpaces_ExtractsCorrectly() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle token with only Bearer prefix")
    void doFilterInternal_OnlyBearerPrefix_NoAuthentication() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Filter Chain Tests ====================

    @Test
    @DisplayName("Should always call filter chain even with no token")
    void doFilterInternal_NoToken_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should always call filter chain even with invalid token")
    void doFilterInternal_InvalidToken_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should always call filter chain even when exception occurs")
    void doFilterInternal_Exception_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== Multiple Roles Tests ====================

    @Test
    @DisplayName("Should authenticate user with multiple roles")
    void doFilterInternal_UserWithMultipleRoles_SetsAllAuthorities() throws ServletException, IOException {
        // Given
        UserDetails adminUser = User.builder()
                .username(testEmail)
                .password("password")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(adminUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .hasSize(2)
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        verify(filterChain).doFilter(request, response);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle very long token")
    void doFilterInternal_VeryLongToken_HandlesCorrectly() throws ServletException, IOException {
        // Given
        String longToken = "a".repeat(1000);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + longToken);
        when(jwtTokenProvider.validateToken(longToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(longToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(jwtTokenProvider).validateToken(longToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void doFilterInternal_EmailWithSpecialCharacters_HandlesCorrectly() throws ServletException, IOException {
        // Given
        String specialEmail = "test+tag@example.co.uk";
        UserDetails specialUserDetails = User.builder()
                .username(specialEmail)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(validToken)).thenReturn(specialEmail);
        when(userDetailsService.loadUserByUsername(specialEmail)).thenReturn(specialUserDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(specialEmail);
        verify(filterChain).doFilter(request, response);
    }
}
