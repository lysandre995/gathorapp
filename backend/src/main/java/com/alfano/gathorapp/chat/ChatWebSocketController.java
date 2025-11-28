package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket controller for real-time chat messaging.
 *
 * Message flow:
 * 1. Client sends message to /app/chat/{outingId}/send
 * 2. Server validates and persists the message
 * 3. Server broadcasts message to /topic/chat/{chatId}
 * 4. All connected participants receive the message in real-time
 *
 * Connection:
 * - Clients must connect to /ws endpoint with valid JWT token
 * - Clients subscribe to /topic/chat/{chatId} to receive messages
 * - Clients send messages to /app/chat/{outingId}/send
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming chat messages via WebSocket.
     *
     * @param outingId     ID of the outing
     * @param request      Message content
     * @param securityUser Authenticated user
     */
    @MessageMapping("/chat/{outingId}/send")
    public void sendMessage(
            @DestinationVariable UUID outingId,
            @Payload SendMessageRequest request,
            Principal principal) {

        log.info("=== WebSocket message received ===");
        log.info("Outing ID: {}", outingId);
        log.info("Principal type: {}", principal != null ? principal.getClass().getName() : "null");

        // Extract SecurityUser from Principal
        SecurityUser securityUser = getSecurityUser(principal);
        UUID userId = securityUser.getUserId();

        log.info("User ID: {}", userId);
        log.info("Message content: {}", request.getContent());

        try {
            // Validate and persist message
            ChatMessageResponse messageResponse = chatService.sendMessage(outingId, request, userId);

            // Get chat ID to broadcast to correct topic
            UUID chatId = messageResponse.getChatId();

            log.info("Message saved with ID: {}", messageResponse.getId());
            log.info("Chat ID: {}", chatId);
            log.info("Broadcasting to topic: /topic/chat/{}", chatId);

            // Broadcast message to all subscribers of this chat
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId,
                    messageResponse);

            log.info("=== Message broadcast successful to /topic/chat/{} ===", chatId);

        } catch (RuntimeException e) {
            log.error("=== Error sending WebSocket message: {} ===", e.getMessage(), e);

            // Send error message back to sender only
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    new ErrorMessage("Failed to send message: " + e.getMessage()));
        }
    }

    /**
     * Handle typing indicator events.
     * Notifies other chat participants that a user is typing.
     *
     * @param outingId     ID of the outing
     * @param securityUser Authenticated user
     */
    @MessageMapping("/chat/{outingId}/typing")
    public void userTyping(
            @DestinationVariable UUID outingId,
            Principal principal) {

        // Extract SecurityUser from Principal
        SecurityUser securityUser = getSecurityUser(principal);
        UUID userId = securityUser.getUserId();
        log.debug("User {} is typing in outing {}", userId, outingId);

        try {
            // Get chat for this outing
            Chat chat = chatService.getOrCreateChat(outingId);

            // Broadcast typing indicator to all participants (except sender)
            TypingIndicator indicator = new TypingIndicator(
                    userId,
                    securityUser.getUsername());

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chat.getId() + "/typing",
                    indicator);

        } catch (RuntimeException e) {
            log.error("Error handling typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Helper method to extract SecurityUser from Principal.
     * WebSocket authentication sets a UsernamePasswordAuthenticationToken with
     * SecurityUser as principal.
     */
    private SecurityUser getSecurityUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
            Object principalObj = authToken.getPrincipal();
            if (principalObj instanceof SecurityUser securityUser) {
                return securityUser;
            }
        }
        throw new RuntimeException("Unable to extract SecurityUser from principal");
    }

    /**
     * DTO for error messages sent back to clients.
     */
    private record ErrorMessage(String message) {
    }

    /**
     * DTO for typing indicator events.
     */
    private record TypingIndicator(UUID userId, String username) {
    }
}
