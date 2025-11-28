package com.alfano.gathorapp.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing Report entities.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find all reports with a specific status.
     */
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    /**
     * Find all reports about a specific user.
     */
    @Query("SELECT r FROM Report r WHERE r.reportedUser.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByReportedUserId(@Param("userId") UUID userId);

    /**
     * Find all reports submitted by a specific user.
     */
    @Query("SELECT r FROM Report r WHERE r.reporter.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByReporterId(@Param("userId") UUID userId);

    /**
     * Find all pending reports (for admin review).
     */
    default List<Report> findAllPending() {
        return findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING);
    }

    /**
     * Find all reports related to a specific entity.
     */
    @Query("SELECT r FROM Report r WHERE r.relatedEntityId = :entityId AND r.relatedEntityType = :entityType ORDER BY r.createdAt DESC")
    List<Report> findByRelatedEntity(@Param("entityId") UUID entityId, @Param("entityType") String entityType);
}
