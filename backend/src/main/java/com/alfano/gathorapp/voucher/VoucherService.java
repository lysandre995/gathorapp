package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.participation.ParticipationStatus;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.reward.RewardRepository;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.alfano.gathorapp.voucher.dto.VoucherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing vouchers and reward distribution.
 *
 * Automatically creates vouchers for Premium users who bring enough
 * participants to event-linked outings with rewards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final RewardRepository rewardRepository;
    private final OutingRepository outingRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    /**
     * Get all vouchers for a user.
     */
    @Transactional(readOnly = true)
    public List<VoucherResponse> getUserVouchers(UUID userId) {
        log.debug("Fetching vouchers for user {}", userId);

        return voucherRepository.findByUserId(userId)
                .stream()
                .map(voucherMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active (unredeemed, non-expired) vouchers for a user.
     */
    @Transactional(readOnly = true)
    public List<VoucherResponse> getActiveVouchers(UUID userId) {
        log.debug("Fetching active vouchers for user {}", userId);

        return voucherRepository.findActiveVouchersByUserId(userId, LocalDateTime.now())
                .stream()
                .map(voucherMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get voucher by ID.
     */
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(UUID voucherId, UUID userId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + voucherId));

        // Verify ownership
        if (!voucher.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: this voucher belongs to another user");
        }

        return voucherMapper.toResponse(voucher);
    }

    /**
     * Redeem a voucher.
     * Called by business user scanning the QR code.
     */
    @Transactional
    public VoucherResponse redeemVoucher(String qrCode, UUID businessUserId) {
        log.info("Redeeming voucher with QR code: {} by business user: {}", qrCode, businessUserId);

        Voucher voucher = voucherRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new com.alfano.gathorapp.exception.ResourceNotFoundException(
                        "Voucher not found with QR code: " + qrCode));

        // Verify the business user owns this reward
        if (!voucher.getReward().getBusiness().getId().equals(businessUserId)) {
            throw new UnauthorizedVoucherAccessException("Unauthorized: this voucher belongs to a different business");
        }

        // Redeem the voucher (throws IllegalStateException if cannot be redeemed)
        try {
            voucher.redeem();
        } catch (IllegalStateException e) {
            throw new VoucherRedemptionException(e.getMessage(), e);
        }
        voucherRepository.save(voucher);

        log.info("Voucher {} successfully redeemed", voucher.getId());

        return voucherMapper.toResponse(voucher);
    }

    /**
     * Check if a Premium user is eligible for a reward based on their outing
     * participation.
     * Creates a voucher if eligible.
     *
     * @param outingId ID of the outing
     * @param userId   ID of the Premium user
     */
    @Transactional
    public void checkAndIssueVoucher(UUID outingId, UUID userId) {
        log.debug("Checking voucher eligibility for user {} in outing {}", userId, outingId);

        Outing outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new RuntimeException("Outing not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only for event-linked outings
        if (outing.getEvent() == null) {
            log.debug("Outing {} is not linked to an event, skipping voucher check", outingId);
            return;
        }

        // Only for organizers who are Premium
        if (!outing.getOrganizer().getId().equals(userId)) {
            log.debug("User {} is not the organizer, skipping voucher check", userId);
            return;
        }

        if (user.getRole() != Role.PREMIUM) {
            log.debug("User {} is not Premium (role: {}), not eligible for vouchers", userId, user.getRole());
            return;
        }

        // Get rewards for this event
        List<Reward> rewards = rewardRepository.findByEventId(outing.getEvent().getId());

        if (rewards.isEmpty()) {
            log.debug("No rewards configured for event {}", outing.getEvent().getId());
            return;
        }

        // Count approved participants (excluding organizer)
        long participantCount = participationRepository.countByOutingAndStatus(outing, ParticipationStatus.APPROVED);

        log.debug("Outing {} has {} approved participants", outingId, participantCount);

        // Check each reward
        for (Reward reward : rewards) {
            if (participantCount >= reward.getRequiredParticipants()) {
                // Check if voucher already exists
                boolean voucherExists = voucherRepository.findByUserId(userId)
                        .stream()
                        .anyMatch(v -> v.getReward().getId().equals(reward.getId()) &&
                                v.getOuting().getId().equals(outingId));

                if (!voucherExists) {
                    // Create voucher
                    Voucher voucher = Voucher.builder()
                            .user(user)
                            .reward(reward)
                            .outing(outing)
                            .build();

                    voucherRepository.save(voucher);
                    log.info("Voucher created for user {} - reward: {}", userId, reward.getTitle());
                } else {
                    log.debug("Voucher already exists for user {} and reward {}", userId, reward.getId());
                }
            }
        }
    }

    /**
     * Scheduled task to expire old vouchers.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireOldVouchers() {
        log.info("Running voucher expiration scheduler...");

        List<Voucher> expiredVouchers = voucherRepository.findExpiredVouchers(LocalDateTime.now());

        if (expiredVouchers.isEmpty()) {
            log.info("No vouchers to expire");
            return;
        }

        log.info("Expiring {} vouchers", expiredVouchers.size());

        for (Voucher voucher : expiredVouchers) {
            voucher.setStatus(VoucherStatus.EXPIRED);
            voucherRepository.save(voucher);
        }

        log.info("Successfully expired {} vouchers", expiredVouchers.size());
    }
}
