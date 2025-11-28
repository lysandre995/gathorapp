package com.alfano.gathorapp.event;

import com.alfano.gathorapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Event entity.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Find all events created by a specific user.
     */
    List<Event> findByCreator(User creator);

    /**
     * Find all events created by a specific user ID.
     */
    List<Event> findByCreatorId(UUID creatorId);

    /**
     * Find all upcoming events (event date after now).
     */
    @Query("SELECT e FROM Event e WHERE e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    /**
     * Find all past events (event date before now).
     */
    @Query("SELECT e FROM Event e WHERE e.eventDate < :now ORDER BY e.eventDate DESC")
    List<Event> findPastEvents(@Param("now") LocalDateTime now);

    /**
     * Find events within a date range.
     */
    @Query("SELECT e FROM Event e WHERE e.eventDate BETWEEN :startDate AND :endDate ORDER BY e.eventDate ASC")
    List<Event> findEventsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find all events after a specific date.
     * Used for proximity search to filter only upcoming events.
     */
    List<Event> findByEventDateAfter(LocalDateTime date);
}
