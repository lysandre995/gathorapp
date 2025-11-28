package com.alfano.gathorapp.report;

import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user report/flag for inappropriate content or behavior.
 *
 * Reports can be submitted against:
 * - Event creators for misleading advertising
 * - Outing organizers for inappropriate behavior
 * - Outing participants for inappropriate behavior
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who submitted the report.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * User being reported.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    /**
     * Type of report (predefined category).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    /**
     * Additional reason provided by the reporter (free text).
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * ID of the related entity (Event or Outing).
     */
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    /**
     * Type of the related entity ("EVENT" or "OUTING").
     */
    @Column(name = "related_entity_type")
    private String relatedEntityType;

    /**
     * Current status of the report.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * When the report was created.
     */
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    /**
     * When the report was last updated (reviewed/dismissed).
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Admin who reviewed the report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Mark this report as reviewed by an admin.
     */
    public void markAsReviewed(User admin) {
        this.status = ReportStatus.REVIEWED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    /**
     * Dismiss this report without action.
     */
    public void dismiss(User admin) {
        this.status = ReportStatus.DISMISSED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }
}
