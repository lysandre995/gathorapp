package com.alfano.gathorapp.reward;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.reward.dto.CreateRewardRequest;
import com.alfano.gathorapp.reward.dto.RewardResponse;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing rewards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final RewardRepository rewardRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RewardMapper rewardMapper;

    /**
     * Get all rewards for an event.
     */
    @Transactional(readOnly = true)
    public List<RewardResponse> getRewardsByEvent(UUID eventId) {
        log.debug("Fetching rewards for event: {}", eventId);
        return rewardRepository.findByEventId(eventId)
                .stream()
                .map(rewardMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a reward (BUSINESS only).
     */
    @Transactional
    public RewardResponse createReward(CreateRewardRequest request, UUID businessId) {
        log.info("Creating reward for event: {}", request.getEventId());

        // Get business user
        User business = userRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (business.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Only BUSINESS users can create rewards");
        }

        // Get event
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Verify event is created by this business
        if (!event.getCreator().getId().equals(businessId)) {
            throw new RuntimeException("You can only create rewards for your own events");
        }

        // Create reward
        Reward reward = Reward.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requiredParticipants(request.getRequiredParticipants())
                .event(event)
                .business(business)
                .build();

        Reward savedReward = rewardRepository.save(reward);
        log.info("Reward created: {}", savedReward.getId());

        return rewardMapper.toResponse(savedReward);
    }
}
