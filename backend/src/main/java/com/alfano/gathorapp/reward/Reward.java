package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a reward offered by a BUSINESS user for an event.
 * Premium users who bring enough participants receive the reward via QR code.
 */
@Entity
@Table(name = "rewards", indexes = {
        @Index(name = "idx_reward_event", columnList = "event_id"),
        @Index(name = "idx_reward_business", columnList = "business_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Minimum number of participants a Premium user must bring to get this reward.
     */
    @Column(nullable = false)
    private Integer requiredParticipants;

    /**
     * QR code string for redemption.
     * Generated automatically.
     */
    @Column(length = 500)
    private String qrCode;

    /**
     * Event this reward is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Event event;

    /**
     * Business user who created this reward.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User business;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (qrCode == null) {
            // Generate simple QR code string (can be enhanced later)
            qrCode = "REWARD-" + id;
        }
    }
}
