package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Reward entity.
 */
@Repository
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    /**
     * Find all rewards for a specific event.
     */
    List<Reward> findByEvent(Event event);

    /**
     * Find all rewards for a specific event ID.
     */
    List<Reward> findByEventId(UUID eventId);

    /**
     * Find all rewards created by a specific business user.
     */
    List<Reward> findByBusinessId(UUID businessId);
}
