package com.alfano.gathorapp.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications and chat.
 * 
 * Uses STOMP protocol over WebSocket for:
 * - User-specific notifications (/user/queue/notifications)
 * - Chat messages (/topic/chat/{chatId})
 * 
 * Connection flow:
 * 1. Client connects to /ws endpoint
 * 2. Client authenticates via WebSocketAuthInterceptor
 * 3. Client subscribes to /user/queue/notifications for personal notifications
 * 4. Client subscribes to /topic/chat/{chatId} for chat messages
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // Prefix for messages FROM server TO client
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // WebSocket endpoint that clients connect to
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200")
                .withSockJS(); // Fallback for browsers without WebSocket support
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        // Add authentication interceptor
        registration.interceptors(authInterceptor);
    }
}
