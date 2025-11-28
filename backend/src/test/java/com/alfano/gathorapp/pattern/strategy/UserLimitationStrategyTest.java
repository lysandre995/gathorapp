package com.alfano.gathorapp.pattern.strategy;

import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for all UserLimitationStrategy implementations.
 * Tests the Strategy Pattern with all three user roles: USER, PREMIUM, BUSINESS.
 */
@DisplayName("UserLimitationStrategy Tests")
class UserLimitationStrategyTest {

    private BaseUserStrategy baseUserStrategy;
    private PremiumUserStrategy premiumUserStrategy;
    private BusinessUserStrategy businessUserStrategy;
    private UserStrategyFactory strategyFactory;

    private User baseUser;
    private User premiumUser;
    private User businessUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        baseUserStrategy = new BaseUserStrategy();
        premiumUserStrategy = new PremiumUserStrategy();
        businessUserStrategy = new BusinessUserStrategy();
        strategyFactory = new UserStrategyFactory(baseUserStrategy, premiumUserStrategy, businessUserStrategy);

        baseUser = createUser(Role.USER);
        premiumUser = createUser(Role.PREMIUM);
        businessUser = createUser(Role.BUSINESS);
        adminUser = createUser(Role.ADMIN);
    }

    private User createUser(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(role);
        return user;
    }

    // ==================== BaseUserStrategy Tests ====================

    @Test
    @DisplayName("BaseUserStrategy - Should return correct max independent outings")
    void baseStrategy_MaxIndependentOutings() {
        assertThat(baseUserStrategy.getMaxIndependentOutingsPerMonth()).isEqualTo(5);
    }

    @Test
    @DisplayName("BaseUserStrategy - Should return correct max participants")
    void baseStrategy_MaxParticipants() {
        assertThat(baseUserStrategy.getMaxParticipantsPerOuting()).isEqualTo(10);
    }

    @Test
    @DisplayName("BaseUserStrategy - Should allow creating independent outing when under limit")
    void baseStrategy_CanCreateIndependentOuting_UnderLimit() {
        assertThat(baseUserStrategy.canCreateIndependentOuting(baseUser, 4)).isTrue();
        assertThat(baseUserStrategy.canCreateIndependentOuting(baseUser, 0)).isTrue();
    }

    @Test
    @DisplayName("BaseUserStrategy - Should not allow creating independent outing when at limit")
    void baseStrategy_CannotCreateIndependentOuting_AtLimit() {
        assertThat(baseUserStrategy.canCreateIndependentOuting(baseUser, 5)).isFalse();
        assertThat(baseUserStrategy.canCreateIndependentOuting(baseUser, 6)).isFalse();
    }

    @Test
    @DisplayName("BaseUserStrategy - Should allow creating event-linked outing")
    void baseStrategy_CanCreateEventLinkedOuting() {
        assertThat(baseUserStrategy.canCreateEventLinkedOuting(baseUser)).isTrue();
    }

    @Test
    @DisplayName("BaseUserStrategy - Should validate participant count within limit")
    void baseStrategy_ValidateParticipantCount_Success() {
        // Should not throw
        baseUserStrategy.validateParticipantCount(1);
        baseUserStrategy.validateParticipantCount(5);
        baseUserStrategy.validateParticipantCount(10);
    }

    @Test
    @DisplayName("BaseUserStrategy - Should throw exception when participant count exceeds limit")
    void baseStrategy_ValidateParticipantCount_ExceedsLimit() {
        assertThatThrownBy(() -> baseUserStrategy.validateParticipantCount(11))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BASE users can have max 10 participants");

        assertThatThrownBy(() -> baseUserStrategy.validateParticipantCount(100))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Requested: 100");
    }

    // ==================== PremiumUserStrategy Tests ====================

    @Test
    @DisplayName("PremiumUserStrategy - Should return correct max independent outings")
    void premiumStrategy_MaxIndependentOutings() {
        assertThat(premiumUserStrategy.getMaxIndependentOutingsPerMonth()).isEqualTo(10);
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should return effectively unlimited max participants")
    void premiumStrategy_MaxParticipants() {
        assertThat(premiumUserStrategy.getMaxParticipantsPerOuting()).isEqualTo(999);
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should allow creating independent outing when under limit")
    void premiumStrategy_CanCreateIndependentOuting_UnderLimit() {
        assertThat(premiumUserStrategy.canCreateIndependentOuting(premiumUser, 9)).isTrue();
        assertThat(premiumUserStrategy.canCreateIndependentOuting(premiumUser, 0)).isTrue();
        assertThat(premiumUserStrategy.canCreateIndependentOuting(premiumUser, 5)).isTrue();
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should not allow creating independent outing when at limit")
    void premiumStrategy_CannotCreateIndependentOuting_AtLimit() {
        assertThat(premiumUserStrategy.canCreateIndependentOuting(premiumUser, 10)).isFalse();
        assertThat(premiumUserStrategy.canCreateIndependentOuting(premiumUser, 11)).isFalse();
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should allow creating event-linked outing (unlimited)")
    void premiumStrategy_CanCreateEventLinkedOuting() {
        assertThat(premiumUserStrategy.canCreateEventLinkedOuting(premiumUser)).isTrue();
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should validate participant count within limit")
    void premiumStrategy_ValidateParticipantCount_Success() {
        // Should not throw
        premiumUserStrategy.validateParticipantCount(1);
        premiumUserStrategy.validateParticipantCount(50);
        premiumUserStrategy.validateParticipantCount(500);
        premiumUserStrategy.validateParticipantCount(999);
    }

    @Test
    @DisplayName("PremiumUserStrategy - Should throw exception when participant count exceeds limit")
    void premiumStrategy_ValidateParticipantCount_ExceedsLimit() {
        assertThatThrownBy(() -> premiumUserStrategy.validateParticipantCount(1000))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Max participants limit is 999");
    }

    // ==================== BusinessUserStrategy Tests ====================

    @Test
    @DisplayName("BusinessUserStrategy - Should return unlimited independent outings")
    void businessStrategy_MaxIndependentOutings() {
        assertThat(businessUserStrategy.getMaxIndependentOutingsPerMonth()).isEqualTo(999999);
    }

    @Test
    @DisplayName("BusinessUserStrategy - Should return unlimited max participants")
    void businessStrategy_MaxParticipants() {
        assertThat(businessUserStrategy.getMaxParticipantsPerOuting()).isEqualTo(999999);
    }

    @Test
    @DisplayName("BusinessUserStrategy - Should always allow creating independent outing")
    void businessStrategy_CanCreateIndependentOuting() {
        assertThat(businessUserStrategy.canCreateIndependentOuting(businessUser, 0)).isTrue();
        assertThat(businessUserStrategy.canCreateIndependentOuting(businessUser, 100)).isTrue();
        assertThat(businessUserStrategy.canCreateIndependentOuting(businessUser, 10000)).isTrue();
    }

    @Test
    @DisplayName("BusinessUserStrategy - Should always allow creating event-linked outing")
    void businessStrategy_CanCreateEventLinkedOuting() {
        assertThat(businessUserStrategy.canCreateEventLinkedOuting(businessUser)).isTrue();
    }

    @Test
    @DisplayName("BusinessUserStrategy - Should validate participant count within reasonable limit")
    void businessStrategy_ValidateParticipantCount_Success() {
        // Should not throw
        businessUserStrategy.validateParticipantCount(1);
        businessUserStrategy.validateParticipantCount(100);
        businessUserStrategy.validateParticipantCount(10000);
        businessUserStrategy.validateParticipantCount(999999);
    }

    @Test
    @DisplayName("BusinessUserStrategy - Should throw exception when participant count is unreasonably high")
    void businessStrategy_ValidateParticipantCount_Unreasonable() {
        assertThatThrownBy(() -> businessUserStrategy.validateParticipantCount(1000000))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unreasonably high");
    }

    // ==================== UserStrategyFactory Tests ====================

    @Test
    @DisplayName("Factory - Should return BaseUserStrategy for USER role")
    void factory_GetStrategy_User() {
        UserLimitationStrategy strategy = strategyFactory.getStrategy(baseUser);
        assertThat(strategy).isInstanceOf(BaseUserStrategy.class);
        assertThat(strategy.getMaxIndependentOutingsPerMonth()).isEqualTo(5);
    }

    @Test
    @DisplayName("Factory - Should return PremiumUserStrategy for PREMIUM role")
    void factory_GetStrategy_Premium() {
        UserLimitationStrategy strategy = strategyFactory.getStrategy(premiumUser);
        assertThat(strategy).isInstanceOf(PremiumUserStrategy.class);
        assertThat(strategy.getMaxIndependentOutingsPerMonth()).isEqualTo(10);
    }

    @Test
    @DisplayName("Factory - Should return BusinessUserStrategy for BUSINESS role")
    void factory_GetStrategy_Business() {
        UserLimitationStrategy strategy = strategyFactory.getStrategy(businessUser);
        assertThat(strategy).isInstanceOf(BusinessUserStrategy.class);
        assertThat(strategy.getMaxIndependentOutingsPerMonth()).isEqualTo(999999);
    }

    @Test
    @DisplayName("Factory - Should return BusinessUserStrategy for ADMIN role")
    void factory_GetStrategy_Admin() {
        UserLimitationStrategy strategy = strategyFactory.getStrategy(adminUser);
        assertThat(strategy).isInstanceOf(BusinessUserStrategy.class);
        assertThat(strategy.getMaxIndependentOutingsPerMonth()).isEqualTo(999999);
    }

    @Test
    @DisplayName("Factory - Should return same strategy for Role parameter")
    void factory_GetStrategy_ByRole() {
        UserLimitationStrategy userStrategy = strategyFactory.getStrategy(Role.USER);
        UserLimitationStrategy premiumStrategy = strategyFactory.getStrategy(Role.PREMIUM);
        UserLimitationStrategy businessStrategy = strategyFactory.getStrategy(Role.BUSINESS);

        assertThat(userStrategy).isInstanceOf(BaseUserStrategy.class);
        assertThat(premiumStrategy).isInstanceOf(PremiumUserStrategy.class);
        assertThat(businessStrategy).isInstanceOf(BusinessUserStrategy.class);
    }

    // ==================== Comparative Tests ====================

    @Test
    @DisplayName("Comparison - Premium should have more outings than Base")
    void comparison_PremiumVsBase_Outings() {
        assertThat(premiumUserStrategy.getMaxIndependentOutingsPerMonth())
                .isGreaterThan(baseUserStrategy.getMaxIndependentOutingsPerMonth());
    }

    @Test
    @DisplayName("Comparison - Business should have more outings than Premium")
    void comparison_BusinessVsPremium_Outings() {
        assertThat(businessUserStrategy.getMaxIndependentOutingsPerMonth())
                .isGreaterThan(premiumUserStrategy.getMaxIndependentOutingsPerMonth());
    }

    @Test
    @DisplayName("Comparison - Premium should have more participants than Base")
    void comparison_PremiumVsBase_Participants() {
        assertThat(premiumUserStrategy.getMaxParticipantsPerOuting())
                .isGreaterThan(baseUserStrategy.getMaxParticipantsPerOuting());
    }

    @Test
    @DisplayName("Comparison - Business should have more participants than Premium")
    void comparison_BusinessVsPremium_Participants() {
        assertThat(businessUserStrategy.getMaxParticipantsPerOuting())
                .isGreaterThan(premiumUserStrategy.getMaxParticipantsPerOuting());
    }

    @Test
    @DisplayName("Comparison - All roles should allow event-linked outings")
    void comparison_AllRolesAllowEventLinked() {
        assertThat(baseUserStrategy.canCreateEventLinkedOuting(baseUser)).isTrue();
        assertThat(premiumUserStrategy.canCreateEventLinkedOuting(premiumUser)).isTrue();
        assertThat(businessUserStrategy.canCreateEventLinkedOuting(businessUser)).isTrue();
    }
}
