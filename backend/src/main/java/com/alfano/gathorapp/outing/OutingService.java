package com.alfano.gathorapp.outing;

import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.exception.ResourceNotFoundException;
import com.alfano.gathorapp.outing.dto.CreateOutingRequest;
import com.alfano.gathorapp.outing.dto.OutingResponse;
import com.alfano.gathorapp.pattern.strategy.UserLimitationStrategy;
import com.alfano.gathorapp.pattern.strategy.UserStrategyFactory;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing outings.
 * Uses Strategy Pattern for user limitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutingService {

    private final OutingRepository outingRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OutingMapper outingMapper;
    private final UserStrategyFactory strategyFactory;

    /**
     * Get all outings.
     */
    @Transactional(readOnly = true)
    public List<OutingResponse> getAllOutings() {
        log.debug("Fetching all outings");
        return outingRepository.findAll()
                .stream()
                .map(outingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all upcoming outings.
     */
    @Transactional(readOnly = true)
    public List<OutingResponse> getUpcomingOutings() {
        log.debug("Fetching upcoming outings");
        return outingRepository.findUpcomingOutings(LocalDateTime.now())
                .stream()
                .map(outingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get outing by ID.
     */
    @Transactional(readOnly = true)
    public OutingResponse getOutingById(UUID id) {
        return getOutingById(id, null);
    }

    /**
     * Get outing by ID with user context.
     * @param id Outing ID
     * @param currentUserId Current user ID for participant check (optional)
     */
    @Transactional(readOnly = true)
    public OutingResponse getOutingById(UUID id, UUID currentUserId) {
        log.debug("Fetching outing with id: {}", id);
        Outing outing = outingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outing not found with id: " + id));
        return outingMapper.toResponse(outing, currentUserId);
    }

    /**
     * Get all outings organized by a specific user.
     */
    @Transactional(readOnly = true)
    public List<OutingResponse> getOutingsByOrganizer(UUID organizerId) {
        log.debug("Fetching outings for organizer: {}", organizerId);
        return outingRepository.findByOrganizerId(organizerId)
                .stream()
                .map(outingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all outings linked to a specific event.
     */
    @Transactional(readOnly = true)
    public List<OutingResponse> getOutingsByEvent(UUID eventId) {
        log.debug("Fetching outings for event: {}", eventId);
        return outingRepository.findByEventId(eventId)
                .stream()
                .map(outingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new outing.
     * Uses Strategy Pattern to apply user limitations.
     */
    @Transactional
    public OutingResponse createOuting(CreateOutingRequest request, UUID organizerId) {
        log.info("Creating new outing: {}", request.getTitle());

        // Get organizer
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + organizerId));

        // Get strategy for this user
        UserLimitationStrategy strategy = strategyFactory.getStrategy(organizer);

        // Validate participant count using strategy
        strategy.validateParticipantCount(request.getMaxParticipants());

        // Get event if linked
        Event event = null;
        boolean isEventLinked = request.getEventId() != null;

        if (isEventLinked) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + request.getEventId()));
            log.debug("Outing linked to event: {}", event.getTitle());

            // Check if user can create event-linked outing
            if (!strategy.canCreateEventLinkedOuting(organizer)) {
                throw new RuntimeException("You cannot create event-linked outings with your current role");
            }
        } else {
            // Independent outing - check monthly limit
            int currentYear = LocalDateTime.now().getYear();
            int currentMonth = LocalDateTime.now().getMonthValue();
            long outingsThisMonth = outingRepository.countByOrganizerInMonth(organizer, currentYear, currentMonth);

            if (!strategy.canCreateIndependentOuting(organizer, outingsThisMonth)) {
                throw new RuntimeException(
                        String.format("You have reached your monthly limit of %d independent outings",
                                strategy.getMaxIndependentOutingsPerMonth()));
            }
        }

        // Create outing
        Outing outing = outingMapper.toEntity(request, organizer, event);
        Outing savedOuting = outingRepository.save(outing);

        log.info("Outing created successfully: {}", savedOuting.getId());
        return outingMapper.toResponse(savedOuting);
    }

    /**
     * Delete an outing.
     * Only the organizer can delete their outing.
     */
    @Transactional
    public void deleteOuting(UUID outingId, UUID userId) {
        log.info("Deleting outing: {}", outingId);

        // Get outing
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new RuntimeException("Outing not found with id: " + outingId));

        // Check if user is the organizer
        if (!outing.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Only the outing organizer can delete this outing");
        }

        outingRepository.delete(outing);
        log.info("Outing deleted successfully: {}", outingId);
    }

    /**
     * Join an outing as a participant.
     * @param outingId ID of the outing to join
     * @param userId ID of the user joining
     * @return Updated outing response
     */
    @Transactional
    public OutingResponse joinOuting(UUID outingId, UUID userId) {
        log.info("User {} joining outing {}", userId, outingId);

        // Get outing
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new RuntimeException("Outing not found with id: " + outingId));

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if user is the organizer
        if (outing.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Organizer cannot join their own outing");
        }

        // Check if outing is full
        if (outing.isFull()) {
            throw new RuntimeException("Outing is full");
        }

        // Check if already a participant
        if (outing.hasParticipant(user)) {
            throw new RuntimeException("User is already a participant");
        }

        // Add participant
        outing.addParticipant(user);
        Outing savedOuting = outingRepository.save(outing);

        log.info("User {} joined outing {} successfully", userId, outingId);
        return outingMapper.toResponse(savedOuting, userId);
    }

    /**
     * Leave an outing.
     * @param outingId ID of the outing to leave
     * @param userId ID of the user leaving
     * @return Updated outing response
     */
    @Transactional
    public OutingResponse leaveOuting(UUID outingId, UUID userId) {
        log.info("User {} leaving outing {}", userId, outingId);

        // Get outing
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new RuntimeException("Outing not found with id: " + outingId));

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if user is a participant
        if (!outing.hasParticipant(user)) {
            throw new RuntimeException("User is not a participant of this outing");
        }

        // Remove participant
        outing.removeParticipant(user);
        Outing savedOuting = outingRepository.save(outing);

        log.info("User {} left outing {} successfully", userId, outingId);
        return outingMapper.toResponse(savedOuting, userId);
    }
}
