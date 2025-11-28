package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Outing entity.
 */
@Repository
public interface OutingRepository extends JpaRepository<Outing, UUID> {

    /**
     * Find all outings organized by a specific user.
     */
    List<Outing> findByOrganizer(User organizer);

    /**
     * Find all outings organized by a specific user ID.
     */
    List<Outing> findByOrganizerId(UUID organizerId);

    /**
     * Find all outings linked to a specific event.
     */
    List<Outing> findByEvent(Event event);

    /**
     * Find all outings linked to a specific event ID.
     */
    List<Outing> findByEventId(UUID eventId);

    /**
     * Find all upcoming outings (outing date after now).
     */
    @Query("SELECT o FROM Outing o WHERE o.outingDate > :now ORDER BY o.outingDate ASC")
    List<Outing> findUpcomingOutings(@Param("now") LocalDateTime now);

    /**
     * Find all independent outings (not linked to any event).
     */
    @Query("SELECT o FROM Outing o WHERE o.event IS NULL ORDER BY o.outingDate ASC")
    List<Outing> findIndependentOutings();

    /**
     * Count outings created by a user in a specific month.
     */
    @Query("SELECT COUNT(o) FROM Outing o WHERE o.organizer = :organizer " +
            "AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    long countByOrganizerInMonth(
            @Param("organizer") User organizer,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * Find all outings after a specific date.
     * Used for proximity search to filter only upcoming outings.
     */
    List<Outing> findByOutingDateAfter(LocalDateTime date);
}
