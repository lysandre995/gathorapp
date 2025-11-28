package com.alfano.gathorapp.report;

/**
 * Status of a report in the review process.
 */
public enum ReportStatus {
    /**
     * Report has been submitted and is waiting for admin review.
     */
    PENDING,

    /**
     * Report has been reviewed by an admin.
     */
    REVIEWED,

    /**
     * Report has been dismissed without action.
     */
    DISMISSED
}
