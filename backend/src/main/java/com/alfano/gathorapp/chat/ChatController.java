package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for chat management.
 * 
 * Endpoints:
 * - GET /api/chats/outing/{outingId}/messages → Get all messages
 * - POST /api/chats/outing/{outingId}/messages → Send a message
 */
@Tag(name = "Chat", description = "Chat management APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * GET /api/chats/outing/{outingId}
     * Get chat details for an outing (returns chatId).
     */
    @Operation(summary = "Get chat info", description = "Get chat information for an outing, including the chat ID needed for WebSocket subscription.")
    @GetMapping("/outing/{outingId}")
    public ResponseEntity<ChatInfoResponse> getChatInfo(
            @PathVariable("outingId") UUID outingId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/chats/outing/{} - User: {}", outingId, userId);
        Chat chat = chatService.getOrCreateChat(outingId);
        ChatInfoResponse response = new ChatInfoResponse(chat.getId(), chat.getOuting().getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chats/outing/{outingId}/messages
     * Get all messages for an outing's chat.
     */
    @Operation(summary = "Get chat messages", description = "Get all messages for an outing's chat. Only participants and organizer can view.")
    @GetMapping("/outing/{outingId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable("outingId") UUID outingId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("GET /api/chats/outing/{}/messages - User: {}", outingId, userId);
        List<ChatMessageResponse> messages = chatService.getMessages(outingId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Simple DTO for chat information.
     */
    public record ChatInfoResponse(UUID chatId, UUID outingId) {}

    /**
     * POST /api/chats/outing/{outingId}/messages
     * Send a message to the outing's chat.
     */
    @Operation(summary = "Send message", description = "Send a message to an outing's chat. Only participants and organizer can send.")
    @PostMapping("/outing/{outingId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("outingId") UUID outingId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        UUID userId = securityUser.getUserId();
        log.info("POST /api/chats/outing/{}/messages - User: {}", outingId, userId);
        ChatMessageResponse message = chatService.sendMessage(outingId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}