package com.alfano.gathorapp.participation;

/**
 * Status of a participation request.
 */
public enum ParticipationStatus {
    /**
     * Participation request is pending organizer approval.
     */
    PENDING,

    /**
     * Participation request has been approved by the organizer.
     */
    APPROVED,

    /**
     * Participation request has been rejected by the organizer.
     */
    REJECTED
}
