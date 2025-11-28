package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.User;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for BASE users.
 * 
 * Limitations:
 * - Max 5 independent outings per month
 * - Max 10 participants per outing
 * - Cannot create unlimited event-linked outings
 */
@Component("baseUserStrategy")
public class BaseUserStrategy implements UserLimitationStrategy {

    private static final int MAX_INDEPENDENT_OUTINGS = 5;
    private static final int MAX_PARTICIPANTS = 10;

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
        // Base users can create event-linked outings but still count toward monthly
        // limit
        return true;
    }

    @Override
    public void validateParticipantCount(int requestedParticipants) {
        if (requestedParticipants > MAX_PARTICIPANTS) {
            throw new RuntimeException(
                    String.format("BASE users can have max %d participants per outing. Requested: %d",
                            MAX_PARTICIPANTS, requestedParticipants));
        }
    }
}
