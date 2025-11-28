package com.alfano.gathorapp.notification;

/**
 * Types of notifications that can be sent to users.
 */
public enum NotificationType {
    /**
     * New participation request received (for organizers).
     */
    PARTICIPATION_REQUEST,

    /**
     * Participation request approved.
     */
    PARTICIPATION_APPROVED,

    /**
     * Participation request rejected.
     */
    PARTICIPATION_REJECTED,

    /**
     * New message in an outing chat.
     */
    NEW_MESSAGE,

    /**
     * Outing is starting soon (reminder).
     */
    OUTING_REMINDER,

    /**
     * New event published near user's location.
     */
    NEW_EVENT_NEARBY,

    /**
     * Event or outing was updated.
     */
    ENTITY_UPDATED,

    /**
     * Reward earned by Premium user.
     */
    REWARD_EARNED,

    /**
     * General system notification.
     */
    SYSTEM,

    /**
     * New report submitted (for admins).
     */
    NEW_REPORT
}
