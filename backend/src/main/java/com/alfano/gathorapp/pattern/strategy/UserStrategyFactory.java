package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for getting the appropriate UserLimitationStrategy based on user
 * role.
 * 
 * This centralizes the strategy selection logic and makes it easy to add new
 * roles.
 */
@Component
@RequiredArgsConstructor
public class UserStrategyFactory {

    private final BaseUserStrategy baseUserStrategy;
    private final PremiumUserStrategy premiumUserStrategy;
    private final BusinessUserStrategy businessUserStrategy;

    /**
     * Get the appropriate strategy for a user based on their role.
     * 
     * @param user the user
     * @return the appropriate limitation strategy
     */
    public UserLimitationStrategy getStrategy(User user) {
        return getStrategy(user.getRole());
    }

    /**
     * Get the appropriate strategy for a role.
     * 
     * @param role the user role
     * @return the appropriate limitation strategy
     */
    public UserLimitationStrategy getStrategy(Role role) {
        return switch (role) {
            case USER -> baseUserStrategy;
            case PREMIUM -> premiumUserStrategy;
            case BUSINESS, ADMIN -> businessUserStrategy; // Business and Admin have no limits
        };
    }
}
