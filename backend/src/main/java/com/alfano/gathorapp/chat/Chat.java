package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.outing.Outing;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a chat for an outing.
 * Each outing has one chat where participants can communicate.
 * Chat is automatically deactivated after a certain period for privacy.
 */
@Entity
@Table(name = "chats", indexes = {
        @Index(name = "idx_chat_outing", columnList = "outing_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The outing this chat belongs to.
     * One-to-one relationship.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outing_id", nullable = false, unique = true)
    private Outing outing;

    /**
     * Whether this chat is still active.
     * Automatically deactivated after outing date + grace period.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * When the chat was deactivated.
     */
    private LocalDateTime deactivatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Deactivate this chat.
     */
    public void deactivate() {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
    }
}