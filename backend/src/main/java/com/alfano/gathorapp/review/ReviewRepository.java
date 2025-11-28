package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Find all reviews for a specific event.
     */
    List<Review> findByEvent(Event event);

    /**
     * Find all reviews for a specific event ID.
     */
    List<Review> findByEventId(UUID eventId);

    /**
     * Find all reviews for a specific outing.
     */
    List<Review> findByOuting(Outing outing);

    /**
     * Find all reviews for a specific outing ID.
     */
    List<Review> findByOutingId(UUID outingId);

    /**
     * Find all reviews by a specific user.
     */
    List<Review> findByReviewer(User reviewer);

    /**
     * Check if user already reviewed an event.
     */
    boolean existsByReviewerAndEvent(User reviewer, Event event);

    /**
     * Check if user already reviewed an outing.
     */
    boolean existsByReviewerAndOuting(User reviewer, Outing outing);

    /**
     * Find review by user and event.
     */
    Optional<Review> findByReviewerAndEvent(User reviewer, Event event);

    /**
     * Find review by user and outing.
     */
    Optional<Review> findByReviewerAndOuting(User reviewer, Outing outing);

    /**
     * Calculate average rating for an event.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.event = :event")
    Double getAverageRatingForEvent(@Param("event") Event event);

    /**
     * Calculate average rating for an outing.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.outing = :outing")
    Double getAverageRatingForOuting(@Param("outing") Outing outing);
}
