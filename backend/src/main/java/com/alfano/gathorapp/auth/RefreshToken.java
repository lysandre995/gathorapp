package com.alfano.gathorapp.auth;

import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a refresh token for JWT authentication.
 * Stores hashed tokens with expiration and revocation support.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_token_hash", columnList = "token_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the refresh token for security.
     */
    @Column(nullable = false, length = 64, name = "token_hash")
    private String tokenHash;

    /**
     * Expiration timestamp in epoch seconds.
     */
    @Column(nullable = false)
    private Long expiresAt;

    /**
     * Whether this token has been revoked (for token rotation).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if this refresh token is still valid.
     */
    public boolean isValid() {
        return !revoked && expiresAt > System.currentTimeMillis() / 1000;
    }
}
