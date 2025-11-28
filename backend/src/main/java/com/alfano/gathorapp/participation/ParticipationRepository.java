package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Participation entity.
 * Includes pessimistic locking for concurrent access control.
 */
@Repository
public interface ParticipationRepository extends JpaRepository<Participation, UUID> {

    /**
     * Find all participations for a specific outing.
     */
    List<Participation> findByOuting(Outing outing);

    /**
     * Find all participations for a specific outing ID.
     */
    List<Participation> findByOutingId(UUID outingId);

    /**
     * Find all participations by a specific user.
     */
    List<Participation> findByUser(User user);

    /**
     * Find all participations by a specific user ID.
     */
    List<Participation> findByUserId(UUID userId);

    /**
     * Check if a user is already participating in an outing.
     */
    boolean existsByUserAndOuting(User user, Outing outing);

    /**
     * Find participation by user and outing.
     */
    Optional<Participation> findByUserAndOuting(User user, Outing outing);

    /**
     * Count approved participations for an outing.
     * Uses pessimistic lock to prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(p) FROM Participation p WHERE p.outing = :outing AND p.status = 'APPROVED'")
    long countApprovedByOuting(@Param("outing") Outing outing);

    /**
     * Find all approved participations for an outing.
     */
    @Query("SELECT p FROM Participation p WHERE p.outing = :outing AND p.status = 'APPROVED'")
    List<Participation> findApprovedByOuting(@Param("outing") Outing outing);

    /**
     * Find all pending participations for an outing.
     */
    @Query("SELECT p FROM Participation p WHERE p.outing = :outing AND p.status = 'PENDING'")
    List<Participation> findPendingByOuting(@Param("outing") Outing outing);

    /**
     * Count participations by outing and status.
     */
    long countByOutingAndStatus(Outing outing, ParticipationStatus status);
}
