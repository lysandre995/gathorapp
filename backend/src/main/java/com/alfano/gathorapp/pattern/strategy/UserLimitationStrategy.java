package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.User;

/**
 * Strategy Pattern interface for user limitations.
 * 
 * Different user roles (USER, PREMIUM, BUSINESS) have different limitations
 * on how many outings they can create and how many participants they can have.
 * 
 * This pattern allows us to:
 * - Encapsulate role-specific behavior
 * - Make the code more maintainable and testable
 * - Easily add new user types without modifying existing code
 */
public interface UserLimitationStrategy {

    /**
     * Get the maximum number of independent outings this user can create per month.
     * 
     * @return max outings per month
     */
    int getMaxIndependentOutingsPerMonth();

    /**
     * Get the maximum number of participants allowed per outing.
     * 
     * @return max participants per outing
     */
    int getMaxParticipantsPerOuting();

    /**
     * Check if user can create an independent outing (not linked to an event).
     * 
     * @param user                     the user
     * @param currentMonthOutingsCount how many outings created this month
     * @return true if user can create an outing
     */
    boolean canCreateIndependentOuting(User user, long currentMonthOutingsCount);

    /**
     * Check if user can create an outing linked to an event.
     * Premium users have unlimited event-linked outings.
     * 
     * @param user the user
     * @return true if user can create event-linked outing
     */
    boolean canCreateEventLinkedOuting(User user);

    /**
     * Validate the requested number of participants.
     * 
     * @param requestedParticipants requested number
     * @throws RuntimeException if exceeds limit
     */
    void validateParticipantCount(int requestedParticipants);
}
