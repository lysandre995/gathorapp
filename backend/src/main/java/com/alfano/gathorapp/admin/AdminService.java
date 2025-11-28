package com.alfano.gathorapp.admin;

import com.alfano.gathorapp.chat.ChatDeactivationScheduler;
import com.alfano.gathorapp.chat.ChatRepository;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserMapper;
import com.alfano.gathorapp.user.UserRepository;
import com.alfano.gathorapp.user.dto.UserResponse;
import com.alfano.gathorapp.voucher.VoucherRepository;
import com.alfano.gathorapp.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for administrative operations.
 *
 * Provides user management, moderation, and system statistics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

        private final UserRepository userRepository;
        private final EventRepository eventRepository;
        private final OutingRepository outingRepository;
        private final ParticipationRepository participationRepository;
        private final ChatRepository chatRepository;
        private final VoucherRepository voucherRepository;
        private final UserMapper userMapper;
        private final ChatDeactivationScheduler chatDeactivationScheduler;
        private final VoucherService voucherService;

        /**
         * Get all users in the system.
         */
        @Transactional(readOnly = true)
        public List<UserResponse> getAllUsers() {
                log.debug("Fetching all users");

                return userRepository.findAll()
                                .stream()
                                .map(userMapper::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Change a user's role.
         */
        @Transactional
        public UserResponse changeUserRole(UUID userId, Role newRole) {
                log.info("Changing role for user {} to {}", userId, newRole);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                Role oldRole = user.getRole();
                user.setRole(newRole);

                User savedUser = userRepository.save(user);
                log.info("User {} role changed from {} to {}", userId, oldRole, newRole);

                return userMapper.toResponse(savedUser);
        }

        /**
         * Delete a user and all associated data.
         */
        @Transactional
        public void deleteUser(UUID userId) {
                log.info("Deleting user {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                // Delete associated data
                // Note: Cascading deletes should be configured in entities for production
                userRepository.delete(user);

                log.info("User {} deleted successfully", userId);
        }

        /**
         * Ban a user from the platform.
         */
        @Transactional
        public UserResponse banUser(UUID userId, String reason) {
                log.info("Banning user {} - Reason: {}", userId, reason);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                user.setBanned(true);
                userRepository.save(user);

                return userMapper.toResponse(user);
        }

        /**
         * Unban a previously banned user.
         */
        @Transactional
        public UserResponse unbanUser(UUID userId) {
                log.info("Unbanning user {}", userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                user.setBanned(false);
                userRepository.save(user);

                return userMapper.toResponse(user);
        }

        /**
         * Get application-wide statistics.
         */
        @Transactional(readOnly = true)
        public Map<String, Object> getStatistics() {
                log.debug("Generating platform statistics");

                Map<String, Object> stats = new HashMap<>();

                // User statistics
                long totalUsers = userRepository.count();
                long baseUsers = userRepository.countByRole(Role.USER);
                long premiumUsers = userRepository.countByRole(Role.PREMIUM);
                long businessUsers = userRepository.countByRole(Role.BUSINESS);

                stats.put("users", Map.of(
                                "total", totalUsers,
                                "base", baseUsers,
                                "premium", premiumUsers,
                                "business", businessUsers));

                // Event statistics
                long totalEvents = eventRepository.count();
                long upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now()).size();

                stats.put("events", Map.of(
                                "total", totalEvents,
                                "upcoming", upcomingEvents));

                // Outing statistics
                long totalOutings = outingRepository.count();
                long upcomingOutings = outingRepository.findUpcomingOutings(LocalDateTime.now()).size();

                stats.put("outings", Map.of(
                                "total", totalOutings,
                                "upcoming", upcomingOutings));

                // Participation statistics
                long totalParticipations = participationRepository.count();

                stats.put("participations", Map.of(
                                "total", totalParticipations));

                // Chat statistics
                long totalChats = chatRepository.count();
                long activeChats = chatRepository.findByActiveTrue().size();

                stats.put("chats", Map.of(
                                "total", totalChats,
                                "active", activeChats));

                // Voucher statistics
                long totalVouchers = voucherRepository.count();
                long activeVouchers = voucherRepository.countAllActiveVouchers(LocalDateTime.now());

                stats.put("vouchers", Map.of(
                                "total", totalVouchers,
                                "active", activeVouchers));

                stats.put("generated_at", LocalDateTime.now());

                log.info("Statistics generated successfully");

                return stats;
        }

        /**
         * Manually trigger expired chat cleanup.
         */
        @Transactional
        public int cleanupExpiredChats() {
                log.info("Manually triggering expired chat cleanup");

                chatDeactivationScheduler.deactivateExpiredChats();

                // Return count of deactivated chats (would need to modify scheduler to return
                // count)
                return 0; // Placeholder
        }

        /**
         * Manually trigger expired voucher cleanup.
         */
        @Transactional
        public int cleanupExpiredVouchers() {
                log.info("Manually triggering expired voucher cleanup");

                voucherService.expireOldVouchers();

                // Return count of expired vouchers (would need to modify service to return
                // count)
                return 0; // Placeholder
        }
}
