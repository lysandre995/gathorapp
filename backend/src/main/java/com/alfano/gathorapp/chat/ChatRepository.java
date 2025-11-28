package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.outing.Outing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Chat entity.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * Find chat by outing.
     */
    Optional<Chat> findByOuting(Outing outing);

    /**
     * Find chat by outing ID.
     */
    Optional<Chat> findByOutingId(UUID outingId);

    /**
     * Find all active chats.
     */
    List<Chat> findByActiveTrue();

    /**
     * Find all active chats where the outing date has passed the grace period.
     *
     * @param expirationDate Date before which outings are considered expired
     * @return List of chats that should be deactivated
     */
    @Query("SELECT c FROM Chat c WHERE c.active = true AND c.outing.outingDate < :expirationDate")
    List<Chat> findActiveChatsWithExpiredOutings(@Param("expirationDate") LocalDateTime expirationDate);
}