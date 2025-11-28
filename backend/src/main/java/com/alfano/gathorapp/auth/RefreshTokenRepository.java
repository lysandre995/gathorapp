package com.alfano.gathorapp.auth;

import com.alfano.gathorapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a valid (non-revoked) refresh token by its hash.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Revoke all refresh tokens for a specific user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(User user);

    /**
     * Revoke a specific refresh token by hash.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenHash = :tokenHash AND rt.revoked = false")
    void revokeByTokenHash(String tokenHash);
}
