package com.alfano.gathorapp.review;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a review left by a user for an event or outing.
 * Users can review after participating.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviewer_event", columnNames = { "reviewer_id", "event_id" }),
        @UniqueConstraint(name = "uk_reviewer_outing", columnNames = { "reviewer_id", "outing_id" })
}, indexes = {
        @Index(name = "idx_review_event", columnList = "event_id"),
        @Index(name = "idx_review_outing", columnList = "outing_id"),
        @Index(name = "idx_review_reviewer", columnList = "reviewer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who wrote the review.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /**
     * Event being reviewed (optional, either event or outing).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Outing being reviewed (optional, either event or outing).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outing_id")
    private Outing outing;

    /**
     * Rating from 1 to 5.
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Optional comment.
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // Validate that either event or outing is set, but not both
        if ((event == null && outing == null) || (event != null && outing != null)) {
            throw new IllegalStateException("Review must be for either an event or an outing, not both");
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    /**
     * Check if this review is for an event.
     */
    public boolean isEventReview() {
        return event != null;
    }

    /**
     * Check if this review is for an outing.
     */
    public boolean isOutingReview() {
        return outing != null;
    }
}
