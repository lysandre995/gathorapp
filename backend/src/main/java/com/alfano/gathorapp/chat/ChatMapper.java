package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Chat entities and DTOs.
 */
@Component
public class ChatMapper {

    /**
     * Convert ChatMessage entity to ChatMessageResponse DTO.
     */
    public ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .sender(ChatMessageResponse.SenderInfo.builder()
                        .id(message.getSender().getId())
                        .name(message.getSender().getName())
                        .build())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}
