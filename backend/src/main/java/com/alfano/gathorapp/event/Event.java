package com.alfano.gathorapp.event;

import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a public event created by a BUSINESS user.
 * Events can be concerts, workshops, exhibitions, organized aperitifs, etc.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_date", columnList = "event_date"),
        @Index(name = "idx_event_creator", columnList = "creator_id"),
        @Index(name = "idx_event_location", columnList = "latitude, longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String location;

    /**
     * Latitude coordinate of the event location.
     * Used for geolocation and proximity searches.
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * Longitude coordinate of the event location.
     * Used for geolocation and proximity searches.
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * Date and time when the event takes place.
     */
    @Column(nullable = false, name = "event_date")
    private LocalDateTime eventDate;

    /**
     * Business user who created this event.
     * Only BUSINESS role users can create events.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
