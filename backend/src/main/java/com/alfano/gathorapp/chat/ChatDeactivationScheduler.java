package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.outing.Outing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to automatically deactivate chats after their outing has ended.
 *
 * Privacy protection: Chats are automatically deactivated 7 days after the outing date
 * to prevent indefinite message history retention.
 *
 * Runs daily at 2:00 AM to deactivate expired chats.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatDeactivationScheduler {

    private final ChatRepository chatRepository;

    /**
     * Number of days after outing date before chat is deactivated.
     */
    private static final int GRACE_PERIOD_DAYS = 7;

    /**
     * Deactivate chats for outings that ended more than GRACE_PERIOD_DAYS ago.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2:00 AM
    @Transactional
    public void deactivateExpiredChats() {
        log.info("Running chat deactivation scheduler...");

        LocalDateTime expirationDate = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);

        // Find all active chats where outing date is older than grace period
        List<Chat> expiredChats = chatRepository.findActiveChatsWithExpiredOutings(expirationDate);

        if (expiredChats.isEmpty()) {
            log.info("No chats to deactivate");
            return;
        }

        log.info("Found {} chats to deactivate", expiredChats.size());

        for (Chat chat : expiredChats) {
            Outing outing = chat.getOuting();
            log.debug("Deactivating chat {} for outing '{}' (date: {})",
                chat.getId(), outing.getTitle(), outing.getOutingDate());

            chat.deactivate();
            chatRepository.save(chat);
        }

        log.info("Successfully deactivated {} chats", expiredChats.size());
    }

    /**
     * Manually deactivate a specific chat.
     *
     * @param chatId ID of the chat to deactivate
     */
    @Transactional
    public void deactivateChat(java.util.UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat not found with id: " + chatId));

        if (!chat.getActive()) {
            log.warn("Chat {} is already deactivated", chatId);
            return;
        }

        chat.deactivate();
        chatRepository.save(chat);
        log.info("Manually deactivated chat {}", chatId);
    }
}
