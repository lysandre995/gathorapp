package com.alfano.gathorapp.websocket;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.security.SecurityUser;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Interceptor for WebSocket authentication.
 * 
 * Validates JWT token when client connects to WebSocket.
 * Token should be sent in the CONNECT frame headers:
 * - Header name: "Authorization"
 * - Header value: "Bearer {token}"
 * 
 * If token is valid, sets user principal for the WebSocket session.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                String token = authorization.get(0);

                // Remove "Bearer " prefix if present
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                        String email = jwtTokenProvider.getEmailFromToken(token);

                        // Load user from database and create SecurityUser
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                        SecurityUser securityUser = new SecurityUser(user);

                        // Create authentication token with SecurityUser as principal
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                securityUser,
                                null,
                                securityUser.getAuthorities()
                        );

                        // Set authentication for this WebSocket session
                        accessor.setUser(authentication);

                        log.info("WebSocket authenticated for user: {} (ID: {})", email, userId);
                    } else {
                        log.warn("Invalid WebSocket JWT token");
                    }
                } catch (Exception e) {
                    log.error("Error authenticating WebSocket: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket connection attempt without Authorization header");
            }
        }

        return message;
    }
}
