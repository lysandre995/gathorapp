package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.reward.dto.CreateRewardRequest;
import com.alfano.gathorapp.reward.dto.RewardResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for RewardService.
 * Tests CRUD operations, authorization, and business rules for rewards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RewardService Tests")
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RewardMapper rewardMapper;

    @InjectMocks
    private RewardService rewardService;

    private User businessUser;
    private User premiumUser;
    private User regularUser;
    private Event testEvent;
    private Reward testReward;
    private UUID businessId;
    private UUID premiumId;
    private UUID regularId;
    private UUID eventId;
    private UUID rewardId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        premiumId = UUID.randomUUID();
        regularId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        rewardId = UUID.randomUUID();

        businessUser = User.builder()
                .id(businessId)
                .name("Business User")
                .email("business@example.com")
                .role(Role.BUSINESS)
                .build();

        premiumUser = User.builder()
                .id(premiumId)
                .name("Premium User")
                .email("premium@example.com")
                .role(Role.PREMIUM)
                .build();

        regularUser = User.builder()
                .id(regularId)
                .name("Regular User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .creator(businessUser)
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();

        testReward = Reward.builder()
                .id(rewardId)
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .qrCode("REWARD-" + rewardId)
                .event(testEvent)
                .business(businessUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getRewardsByEvent Tests ====================

    @Test
    @DisplayName("Should get all rewards for an event")
    void getRewardsByEvent_Success() {
        when(rewardRepository.findByEventId(eventId)).thenReturn(List.of(testReward));
        when(rewardMapper.toResponse(testReward))
                .thenReturn(RewardResponse.builder()
                        .id(rewardId)
                        .title("Free Coffee")
                        .requiredParticipants(3)
                        .build());

        List<RewardResponse> result = rewardService.getRewardsByEvent(eventId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(rewardId);
        assertThat(result.get(0).getTitle()).isEqualTo("Free Coffee");
        assertThat(result.get(0).getRequiredParticipants()).isEqualTo(3);
        verify(rewardRepository).findByEventId(eventId);
    }

    @Test
    @DisplayName("Should return empty list when no rewards for event")
    void getRewardsByEvent_EmptyList() {
        when(rewardRepository.findByEventId(eventId)).thenReturn(List.of());

        List<RewardResponse> result = rewardService.getRewardsByEvent(eventId);

        assertThat(result).isEmpty();
        verify(rewardRepository).findByEventId(eventId);
    }

    @Test
    @DisplayName("Should return multiple rewards for event")
    void getRewardsByEvent_MultipleRewards() {
        Reward secondReward = Reward.builder()
                .id(UUID.randomUUID())
                .title("Free Meal")
                .description("Free meal for bringing 5 participants")
                .requiredParticipants(5)
                .event(testEvent)
                .business(businessUser)
                .build();

        when(rewardRepository.findByEventId(eventId)).thenReturn(List.of(testReward, secondReward));
        when(rewardMapper.toResponse(testReward))
                .thenReturn(RewardResponse.builder().id(rewardId).build());
        when(rewardMapper.toResponse(secondReward))
                .thenReturn(RewardResponse.builder().id(secondReward.getId()).build());

        List<RewardResponse> result = rewardService.getRewardsByEvent(eventId);

        assertThat(result).hasSize(2);
        verify(rewardRepository).findByEventId(eventId);
        verify(rewardMapper, times(2)).toResponse(any(Reward.class));
    }

    // ==================== createReward Tests ====================

    @Test
    @DisplayName("Should create reward successfully by business user")
    void createReward_Success() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(rewardRepository.save(any(Reward.class))).thenReturn(testReward);
        when(rewardMapper.toResponse(testReward))
                .thenReturn(RewardResponse.builder()
                        .id(rewardId)
                        .title("Free Coffee")
                        .requiredParticipants(3)
                        .build());

        RewardResponse result = rewardService.createReward(request, businessId);

        assertThat(result.getId()).isEqualTo(rewardId);
        assertThat(result.getTitle()).isEqualTo("Free Coffee");
        assertThat(result.getRequiredParticipants()).isEqualTo(3);
        verify(rewardRepository).save(any(Reward.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void createReward_UserNotFound() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.createReward(request, businessId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when non-business user tries to create reward")
    void createReward_NotBusinessUser() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(premiumId)).thenReturn(Optional.of(premiumUser));

        assertThatThrownBy(() -> rewardService.createReward(request, premiumId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only BUSINESS users can create rewards");

        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when regular user tries to create reward")
    void createReward_RegularUserCannotCreate() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(regularId)).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> rewardService.createReward(request, regularId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only BUSINESS users can create rewards");

        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when event not found")
    void createReward_EventNotFound() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.createReward(request, businessId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event not found");

        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when business user tries to create reward for other's event")
    void createReward_NotOwnEvent() {
        User otherBusiness = User.builder()
                .id(UUID.randomUUID())
                .name("Other Business")
                .email("other@example.com")
                .role(Role.BUSINESS)
                .build();

        Event otherEvent = Event.builder()
                .id(eventId)
                .title("Other Event")
                .creator(otherBusiness)
                .build();

        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Free Coffee")
                .description("Get a free coffee for bringing 3 participants")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(otherEvent));

        assertThatThrownBy(() -> rewardService.createReward(request, businessId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You can only create rewards for your own events");

        verify(rewardRepository, never()).save(any());
    }

    // ==================== Business Logic Tests ====================

    @Test
    @DisplayName("Should create reward with minimum required participants (1)")
    void createReward_MinimumParticipants() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Small Reward")
                .description("Reward for bringing just 1 participant")
                .requiredParticipants(1)
                .eventId(eventId)
                .build();

        Reward rewardWithMin = Reward.builder()
                .id(rewardId)
                .title("Small Reward")
                .requiredParticipants(1)
                .event(testEvent)
                .business(businessUser)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(rewardRepository.save(any(Reward.class))).thenReturn(rewardWithMin);
        when(rewardMapper.toResponse(rewardWithMin))
                .thenReturn(RewardResponse.builder()
                        .id(rewardId)
                        .requiredParticipants(1)
                        .build());

        RewardResponse result = rewardService.createReward(request, businessId);

        assertThat(result.getRequiredParticipants()).isEqualTo(1);
        verify(rewardRepository).save(any(Reward.class));
    }

    @Test
    @DisplayName("Should create reward with high required participants")
    void createReward_HighParticipantRequirement() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Premium Reward")
                .description("Exclusive reward for bringing 20 participants")
                .requiredParticipants(20)
                .eventId(eventId)
                .build();

        Reward rewardWithHigh = Reward.builder()
                .id(rewardId)
                .title("Premium Reward")
                .requiredParticipants(20)
                .event(testEvent)
                .business(businessUser)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(rewardRepository.save(any(Reward.class))).thenReturn(rewardWithHigh);
        when(rewardMapper.toResponse(rewardWithHigh))
                .thenReturn(RewardResponse.builder()
                        .id(rewardId)
                        .requiredParticipants(20)
                        .build());

        RewardResponse result = rewardService.createReward(request, businessId);

        assertThat(result.getRequiredParticipants()).isEqualTo(20);
        verify(rewardRepository).save(any(Reward.class));
    }

    @Test
    @DisplayName("Should verify all reward fields are properly set")
    void createReward_AllFieldsSet() {
        CreateRewardRequest request = CreateRewardRequest.builder()
                .title("Complete Reward")
                .description("A reward with all fields properly configured")
                .requiredParticipants(5)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(rewardRepository.save(any(Reward.class))).thenAnswer(invocation -> {
            Reward savedReward = invocation.getArgument(0);
            // Verify all fields are set correctly
            assertThat(savedReward.getTitle()).isEqualTo("Complete Reward");
            assertThat(savedReward.getDescription()).isEqualTo("A reward with all fields properly configured");
            assertThat(savedReward.getRequiredParticipants()).isEqualTo(5);
            assertThat(savedReward.getEvent()).isEqualTo(testEvent);
            assertThat(savedReward.getBusiness()).isEqualTo(businessUser);
            return testReward;
        });
        when(rewardMapper.toResponse(testReward))
                .thenReturn(RewardResponse.builder().id(rewardId).build());

        rewardService.createReward(request, businessId);

        verify(rewardRepository).save(any(Reward.class));
    }

    @Test
    @DisplayName("Should create multiple rewards for same event")
    void createReward_MultipleRewardsForSameEvent() {
        CreateRewardRequest request1 = CreateRewardRequest.builder()
                .title("Bronze Reward")
                .description("Bronze tier reward")
                .requiredParticipants(3)
                .eventId(eventId)
                .build();

        CreateRewardRequest request2 = CreateRewardRequest.builder()
                .title("Gold Reward")
                .description("Gold tier reward")
                .requiredParticipants(10)
                .eventId(eventId)
                .build();

        when(userRepository.findById(businessId)).thenReturn(Optional.of(businessUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(rewardRepository.save(any(Reward.class))).thenReturn(testReward);
        when(rewardMapper.toResponse(testReward))
                .thenReturn(RewardResponse.builder().id(rewardId).build());

        rewardService.createReward(request1, businessId);
        rewardService.createReward(request2, businessId);

        verify(rewardRepository, times(2)).save(any(Reward.class));
    }
}
