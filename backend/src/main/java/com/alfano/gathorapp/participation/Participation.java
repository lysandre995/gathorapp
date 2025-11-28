package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's participation in an outing.
 * Participation can be pending, approved, or rejected by the organizer.
 */
@Entity
@Table(name = "participations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_outing", columnNames = { "user_id", "outing_id" })
}, indexes = {
        @Index(name = "idx_participation_outing", columnList = "outing_id"),
        @Index(name = "idx_participation_user", columnList = "user_id"),
        @Index(name = "idx_participation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who wants to participate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Outing the user wants to join.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outing_id", nullable = false)
    private Outing outing;

    /**
     * Status of the participation request.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.PENDING;

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
     * Check if this participation is approved.
     */
    public boolean isApproved() {
        return status == ParticipationStatus.APPROVED;
    }

    /**
     * Check if this participation is pending.
     */
    public boolean isPending() {
        return status == ParticipationStatus.PENDING;
    }
}
