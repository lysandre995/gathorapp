package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.User;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for PREMIUM users.
 * 
 * Extended limitations:
 * - Max 10 independent outings per month
 * - Unlimited participants per outing
 * - Unlimited event-linked outings (don't count toward monthly limit)
 */
@Component("premiumUserStrategy")
public class PremiumUserStrategy implements UserLimitationStrategy {

    private static final int MAX_INDEPENDENT_OUTINGS = 10;
    private static final int MAX_PARTICIPANTS = 999; // Effectively unlimited

    @Override
    public int getMaxIndependentOutingsPerMonth() {
        return MAX_INDEPENDENT_OUTINGS;
    }

    @Override
    public int getMaxParticipantsPerOuting() {
        return MAX_PARTICIPANTS;
    }

    @Override
    public boolean canCreateIndependentOuting(User user, long currentMonthOutingsCount) {
        return currentMonthOutingsCount < MAX_INDEPENDENT_OUTINGS;
    }

    @Override
    public boolean canCreateEventLinkedOuting(User user) {
        // Premium users have UNLIMITED event-linked outings
        return true;
    }

    @Override
    public void validateParticipantCount(int requestedParticipants) {
        if (requestedParticipants > MAX_PARTICIPANTS) {
            throw new RuntimeException(
                    String.format("Max participants limit is %d. Requested: %d",
                            MAX_PARTICIPANTS, requestedParticipants));
        }
    }
}
