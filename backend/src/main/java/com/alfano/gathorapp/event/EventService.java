package com.alfano.gathorapp.event;

import com.alfano.gathorapp.event.dto.CreateEventRequest;
import com.alfano.gathorapp.event.dto.EventResponse;
import com.alfano.gathorapp.event.dto.UpdateEventRequest;
import com.alfano.gathorapp.user.Role;
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
 * Service for managing events.
 * Only BUSINESS users can create events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    /**
     * Get all events.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        log.debug("Fetching all events");
        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all upcoming events.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        log.debug("Fetching upcoming events");
        return eventRepository.findUpcomingEvents(LocalDateTime.now())
                .stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get event by ID.
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        log.debug("Fetching event with id: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return eventMapper.toResponse(event);
    }

    /**
     * Get all events created by a specific user.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCreator(UUID creatorId) {
        log.debug("Fetching events for creator: {}", creatorId);
        return eventRepository.findByCreatorId(creatorId)
                .stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new event.
     * Only BUSINESS users can create events.
     */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, UUID creatorId) {
        log.info("Creating new event: {}", request.getTitle());

        // Get creator
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + creatorId));

        // Check if user is BUSINESS
        if (creator.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Only BUSINESS users can create events. User role: " + creator.getRole());
        }

        // Create event
        Event event = eventMapper.toEntity(request, creator);
        Event savedEvent = eventRepository.save(event);

        log.info("Event created successfully: {}", savedEvent.getId());
        return eventMapper.toResponse(savedEvent);
    }

    /**
     * Update an existing event.
     * Only the creator can update their event.
     */
    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, UUID userId) {
        log.info("Updating event: {}", eventId);

        // Get event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        // Check if user is the creator
        if (!event.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the event creator can update this event");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: {}", updatedEvent.getId());

        return eventMapper.toResponse(updatedEvent);
    }

    /**
     * Delete an event.
     * Only the creator can delete their event.
     */
    @Transactional
    public void deleteEvent(UUID eventId, UUID userId) {
        log.info("Deleting event: {}", eventId);

        // Get event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        // Check if user is the creator
        if (!event.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the event creator can delete this event");
        }

        eventRepository.delete(event);
        log.info("Event deleted successfully: {}", eventId);
    }
}
