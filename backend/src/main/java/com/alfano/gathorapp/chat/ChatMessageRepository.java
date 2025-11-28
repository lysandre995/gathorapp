package com.alfano.gathorapp.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ChatMessage entity.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find all messages for a specific chat, ordered by timestamp.
     */
    List<ChatMessage> findByChatOrderByTimestampAsc(Chat chat);

    /**
     * Find all messages for a specific chat ID, ordered by timestamp.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId ORDER BY m.timestamp ASC")
    List<ChatMessage> findByChatIdOrderByTimestamp(@Param("chatId") UUID chatId);

    /**
     * Find recent messages (last N messages).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chat = :chat ORDER BY m.timestamp DESC")
    List<ChatMessage> findRecentMessagesByChat(@Param("chat") Chat chat);

    /**
     * Find messages after a specific timestamp.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chat = :chat AND m.timestamp > :after ORDER BY m.timestamp ASC")
    List<ChatMessage> findMessagesByAfter(@Param("chat") Chat chat, @Param("after") LocalDateTime after);
}