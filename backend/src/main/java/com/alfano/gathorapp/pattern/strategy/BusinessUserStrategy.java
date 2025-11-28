package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.User;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for BUSINESS users.
 * 
 * No limitations:
 * - Unlimited independent outings
 * - Unlimited participants per outing
 * - Unlimited event-linked outings
 * 
 * Business users are meant to promote their events and activities.
 */
@Component("businessUserStrategy")
public class BusinessUserStrategy implements UserLimitationStrategy {

    private static final int UNLIMITED = 999999;

    @Override
    public int getMaxIndependentOutingsPerMonth() {
        return UNLIMITED;
    }

    @Override
    public int getMaxParticipantsPerOuting() {
        return UNLIMITED;
    }

    @Override
    public boolean canCreateIndependentOuting(User user, long currentMonthOutingsCount) {
        // Business users have unlimited outings
        return true;
    }

    @Override
    public boolean canCreateEventLinkedOuting(User user) {
        // Business users have unlimited event-linked outings
        return true;
    }

    @Override
    public void validateParticipantCount(int requestedParticipants) {
        // Business users have no practical limit
        if (requestedParticipants > UNLIMITED) {
            throw new RuntimeException("Participant count is unreasonably high");
        }
    }
}
