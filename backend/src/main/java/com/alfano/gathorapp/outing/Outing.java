package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing an outing organized by a user.
 * An outing can be independent or linked to an event.
 * Users can join outings to participate together.
 */
@Entity
@Table(name = "outings", indexes = {
        @Index(name = "idx_outing_date", columnList = "outing_date"),
        @Index(name = "idx_outing_organizer", columnList = "organizer_id"),
        @Index(name = "idx_outing_event", columnList = "event_id"),
        @Index(name = "idx_outing_location", columnList = "latitude, longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outing {

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
     * Latitude coordinate of the outing location.
     * Used for geolocation and proximity searches.
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * Longitude coordinate of the outing location.
     * Used for geolocation and proximity searches.
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * Date and time when the outing takes place.
     */
    @Column(nullable = false, name = "outing_date")
    private LocalDateTime outingDate;

    /**
     * Maximum number of participants allowed.
     * Depends on user role (Base: 10, Premium: unlimited).
     */
    @Column(nullable = false)
    private Integer maxParticipants;

    /**
     * User who organized this outing.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    /**
     * Optional: Event this outing is linked to.
     * If null, the outing is independent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Users who have joined this outing as participants.
     * Does not include the organizer.
     */
    @ManyToMany
    @JoinTable(
        name = "outing_participants",
        joinColumns = @JoinColumn(name = "outing_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> participants = new HashSet<>();

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

    /**
     * Check if this outing is linked to an event.
     */
    public boolean isLinkedToEvent() {
        return event != null;
    }

    /**
     * Add a participant to this outing.
     * @param user User to add
     * @return true if added, false if already present
     */
    public boolean addParticipant(User user) {
        return participants.add(user);
    }

    /**
     * Remove a participant from this outing.
     * @param user User to remove
     * @return true if removed, false if not present
     */
    public boolean removeParticipant(User user) {
        return participants.remove(user);
    }

    /**
     * Check if a user is a participant.
     * @param user User to check
     * @return true if user is a participant
     */
    public boolean hasParticipant(User user) {
        return participants.contains(user);
    }

    /**
     * Get current number of participants (excluding organizer).
     * @return number of participants
     */
    public int getCurrentParticipantCount() {
        return participants.size();
    }

    /**
     * Check if outing is full.
     * @return true if participant limit reached
     */
    public boolean isFull() {
        return getCurrentParticipantCount() >= maxParticipants;
    }
}
