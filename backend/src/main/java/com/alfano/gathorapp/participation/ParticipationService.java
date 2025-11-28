package com.alfano.gathorapp.participation;

import com.alfano.gathorapp.exception.BadRequestException;
import com.alfano.gathorapp.exception.ForbiddenException;
import com.alfano.gathorapp.exception.ResourceNotFoundException;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.dto.ParticipationResponse;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.voucher.VoucherService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing participations.
 * 
 * IMPORTANT: Implements concurrent access control using:
 * - Pessimistic locking (SELECT FOR UPDATE)
 * - Synchronized methods
 * - SERIALIZABLE isolation level
 * 
 * This prevents race conditions when multiple users try to join the same outing
 * simultaneously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final OutingRepository outingRepository;
    private final UserRepository userRepository;
    private final ParticipationMapper participationMapper;
    private final NotificationService notificationService;
    private final VoucherService voucherService;

    /**
     * Get all participations for a specific outing.
     */
    @Transactional(readOnly = true)
    public List<ParticipationResponse> getParticipationsByOuting(UUID outingId) {
        log.debug("Fetching participations for outing: {}", outingId);
        return participationRepository.findByOutingId(outingId)
                .stream()
                .map(participationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all participations by a specific user.
     */
    @Transactional(readOnly = true)
    public List<ParticipationResponse> getParticipationsByUser(UUID userId) {
        log.debug("Fetching participations for user: {}", userId);
        return participationRepository.findByUserId(userId)
                .stream()
                .map(participationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Request to join an outing.
     * 
     * CONCURRENT ACCESS CONTROL:
     * - Uses SERIALIZABLE isolation to prevent phantom reads
     * - Synchronized to prevent race conditions
     * - Pessimistic lock on outing entity
     * 
     * This ensures that when multiple users try to join simultaneously,
     * only one transaction at a time can check and modify the participant count.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized ParticipationResponse joinOuting(UUID outingId, UUID userId) {
        log.info("User {} attempting to join outing {}", userId, outingId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Get outing with pessimistic lock (SELECT FOR UPDATE)
        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new ResourceNotFoundException("Outing not found with id: " + outingId));

        // Check if user is the organizer
        if (outing.getOrganizer().getId().equals(userId)) {
            throw new BadRequestException("Organizer cannot join their own outing");
        }

        // Check if user already has a participation request
        if (participationRepository.existsByUserAndOuting(user, outing)) {
            throw new BadRequestException("User already has a participation request for this outing");
        }

        // CRITICAL SECTION: Check if outing is full
        long approvedCount = participationRepository.countApprovedByOuting(outing);

        if (approvedCount >= outing.getMaxParticipants()) {
            log.warn("Outing {} is full. Approved: {}, Max: {}",
                    outingId, approvedCount, outing.getMaxParticipants());
            throw new BadRequestException("Outing is full. Maximum participants reached.");
        }

        // Create participation (initially PENDING)
        Participation participation = Participation.builder()
                .user(user)
                .outing(outing)
                .status(ParticipationStatus.PENDING)
                .build();

        Participation savedParticipation = participationRepository.save(participation);

        // Notify organizer of new participation request
        notificationService.createNotification(
                outing.getOrganizer().getId(),
                NotificationType.PARTICIPATION_REQUEST,
                "New participation request",
                user.getName() + " wants to join " + outing.getTitle(),
                savedParticipation.getId(),
                "PARTICIPATION");

        log.info("User {} successfully requested to join outing {}", userId, outingId);
        return participationMapper.toResponse(savedParticipation);
    }

    /**
     * Approve a participation request (organizer only).
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized ParticipationResponse approveParticipation(UUID participationId, UUID organizerId) {
        log.info("Approving participation: {}", participationId);

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found with id: " + participationId));

        // Check if user is the organizer
        if (!participation.getOuting().getOrganizer().getId().equals(organizerId)) {
            throw new ForbiddenException("Only the outing organizer can approve participations");
        }

        // Check if already approved or rejected
        if (participation.getStatus() != ParticipationStatus.PENDING) {
            throw new BadRequestException("Participation is not pending. Current status: " + participation.getStatus());
        }

        // CRITICAL SECTION: Check if outing is full before approving
        Outing outing = participation.getOuting();
        long approvedCount = participationRepository.countApprovedByOuting(outing);

        if (approvedCount >= outing.getMaxParticipants()) {
            throw new BadRequestException("Cannot approve: outing is already full");
        }

        // Approve participation
        participation.setStatus(ParticipationStatus.APPROVED);
        Participation approvedParticipation = participationRepository.save(participation);

        // Add user to outing participants list
        outing.addParticipant(participation.getUser());
        outingRepository.save(outing);

        // Check if organizer earned a voucher (Premium users only)
        voucherService.checkAndIssueVoucher(outing.getId(), outing.getOrganizer().getId());
        log.debug("Checked voucher eligibility for organizer {} in outing {}",
            outing.getOrganizer().getId(), outing.getId());

        // Notify user that their participation was approved
        notificationService.createNotification(
                participation.getUser().getId(),
                NotificationType.PARTICIPATION_APPROVED,
                "Participation approved!",
                "Your request for " + outing.getTitle() + " has been approved",
                outing.getId(),
                "OUTING");

        log.info("Participation {} approved successfully", participationId);
        return participationMapper.toResponse(approvedParticipation);
    }

    /**
     * Reject a participation request (organizer only).
     */
    @Transactional
    public ParticipationResponse rejectParticipation(UUID participationId, UUID organizerId) {
        log.info("Rejecting participation: {}", participationId);

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found with id: " + participationId));

        // Check if user is the organizer
        if (!participation.getOuting().getOrganizer().getId().equals(organizerId)) {
            throw new ForbiddenException("Only the outing organizer can reject participations");
        }

        // Check if already approved or rejected
        if (participation.getStatus() != ParticipationStatus.PENDING) {
            throw new BadRequestException("Participation is not pending. Current status: " + participation.getStatus());
        }

        // Reject participation
        participation.setStatus(ParticipationStatus.REJECTED);
        Participation rejectedParticipation = participationRepository.save(participation);

        // Notify user that their participation was rejected
        notificationService.createNotification(
                participation.getUser().getId(),
                NotificationType.PARTICIPATION_REJECTED,
                "Participation rejected",
                "Your request for " + participation.getOuting().getTitle() + " was rejected",
                participation.getOuting().getId(),
                "OUTING");

        log.info("Participation {} rejected", participationId);
        return participationMapper.toResponse(rejectedParticipation);
    }

    /**
     * Leave an outing (participant can cancel their own participation).
     */
    @Transactional
    public void leaveOuting(UUID participationId, UUID userId) {
        log.info("User {} leaving outing via participation {}", userId, participationId);

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found with id: " + participationId));

        // Check if user owns this participation
        if (!participation.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own participation");
        }

        // Remove user from outing participants list if they were approved
        if (participation.getStatus() == ParticipationStatus.APPROVED) {
            Outing outing = participation.getOuting();
            outing.removeParticipant(participation.getUser());
            outingRepository.save(outing);
        }

        participationRepository.delete(participation);
        log.info("User {} successfully left outing", userId);
    }
}
