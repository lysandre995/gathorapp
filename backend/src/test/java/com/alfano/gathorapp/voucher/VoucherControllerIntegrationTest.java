package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.reward.RewardRepository;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VoucherController.
 * Tests voucher generation, retrieval, and redemption.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("VoucherController Integration Tests")
class VoucherControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private VoucherRepository voucherRepository;

        @Autowired
        private RewardRepository rewardRepository;

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private OutingRepository outingRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User premiumUser;
        private User businessUser;
        private Reward testReward;
        private Voucher testVoucher;
        private Outing testOuting;
        private String premiumToken;
        private String businessToken;

        @BeforeEach
        void setUp() {
                // Ensure a clean database state for each test
                testDatabaseCleaner.truncateAll();

                // Create premium user
                premiumUser = User.builder()
                                .name("Premium User")
                                .email("premium@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.PREMIUM)
                                .build();
                premiumUser = userRepository.save(premiumUser);

                // Create business user
                businessUser = User.builder()
                                .name("Business User")
                                .email("business@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.BUSINESS)
                                .build();
                businessUser = userRepository.save(businessUser);

                // Create event for business user
                Event event = Event.builder()
                                .title("Test Event")
                                .description("Test Event Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .eventDate(LocalDateTime.now().plusDays(30))
                                .creator(businessUser)
                                .build();
                event = eventRepository.save(event);

                // Create reward
                testReward = Reward.builder()
                                .title("Test Reward")
                                .description("Test Reward Description")
                                .requiredParticipants(5)
                                .event(event)
                                .business(businessUser)
                                .build();
                testReward = rewardRepository.save(testReward);

                // Create outing
                testOuting = Outing.builder()
                                .title("Test Outing")
                                .description("Test Description")
                                .location("Test Location")
                                .latitude(40.3515)
                                .longitude(18.1750)
                                .outingDate(LocalDateTime.now().plusDays(7))
                                .maxParticipants(10)
                                .organizer(premiumUser)
                                .event(event)
                                .build();
                testOuting = outingRepository.save(testOuting);

                // Create test voucher
                testVoucher = Voucher.builder()
                                .qrCode("TEST-VOUCHER-123")
                                .user(premiumUser)
                                .reward(testReward)
                                .outing(testOuting)
                                .status(VoucherStatus.ACTIVE)
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(60))
                                .build();
                testVoucher = voucherRepository.save(testVoucher);

                // Generate tokens
                premiumToken = jwtTokenProvider.generateAccessToken(
                                premiumUser.getId(), premiumUser.getEmail(), premiumUser.getRole().name());
                businessToken = jwtTokenProvider.generateAccessToken(
                                businessUser.getId(), businessUser.getEmail(), businessUser.getRole().name());
        }

        @Test
        @DisplayName("GET /api/vouchers/my - Should return user vouchers")
        void getMyVouchers_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/vouchers/my")
                                .header("Authorization", "Bearer " + premiumToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].qrCode", is("TEST-VOUCHER-123")))
                                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("GET /api/vouchers/my/active - Should return only active vouchers")
        void getActiveVouchers_Success() throws Exception {
                // Given - create another expired voucher
                Voucher expiredVoucher = Voucher.builder()
                                .qrCode("EXPIRED-VOUCHER")
                                .user(premiumUser)
                                .reward(testReward)
                                .outing(testOuting)
                                .status(VoucherStatus.EXPIRED)
                                .issuedAt(LocalDateTime.now().minusDays(70))
                                .expiresAt(LocalDateTime.now().minusDays(10))
                                .build();
                voucherRepository.save(expiredVoucher);

                // When & Then
                mockMvc.perform(get("/api/vouchers/my/active")
                                .header("Authorization", "Bearer " + premiumToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("GET /api/vouchers/{id} - Should return voucher by id")
        void getVoucherById_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/vouchers/" + testVoucher.getId())
                                .header("Authorization", "Bearer " + premiumToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(testVoucher.getId().toString())))
                                .andExpect(jsonPath("$.qrCode", is("TEST-VOUCHER-123")))
                                .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("POST /api/vouchers/redeem/{qrCode} - Business user should redeem voucher")
        void redeemVoucher_Success() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/vouchers/redeem/" + testVoucher.getQrCode())
                                .header("Authorization", "Bearer " + businessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("REDEEMED")));
        }

        @Test
        @DisplayName("POST /api/vouchers/redeem/{qrCode} - Should fail if already redeemed")
        void redeemVoucher_AlreadyRedeemed() throws Exception {
                // Given - redeem once
                mockMvc.perform(post("/api/vouchers/redeem/" + testVoucher.getQrCode())
                                .header("Authorization", "Bearer " + businessToken))
                                .andExpect(status().isOk());

                // When & Then - try to redeem again
                mockMvc.perform(post("/api/vouchers/redeem/" + testVoucher.getQrCode())
                                .header("Authorization", "Bearer " + businessToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/vouchers/redeem/{qrCode} - Non-business user should fail")
        void redeemVoucher_NotBusiness() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/vouchers/redeem/" + testVoucher.getQrCode())
                                .header("Authorization", "Bearer " + premiumToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/vouchers/redeem/{qrCode} - Should fail if voucher expired")
        void redeemVoucher_Expired() throws Exception {
                // Given - create expired voucher
                Voucher expiredVoucher = Voucher.builder()
                                .qrCode("EXPIRED-VOUCHER")
                                .user(premiumUser)
                                .reward(testReward)
                                .outing(testOuting)
                                .status(VoucherStatus.EXPIRED)
                                .issuedAt(LocalDateTime.now().minusDays(70))
                                .expiresAt(LocalDateTime.now().minusDays(10))
                                .build();
                voucherRepository.save(expiredVoucher);

                // When & Then
                mockMvc.perform(post("/api/vouchers/redeem/" + expiredVoucher.getQrCode())
                                .header("Authorization", "Bearer " + businessToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/vouchers/my - Should return empty list for user with no vouchers")
        void getMyVouchers_EmptyList() throws Exception {
                // Given - create user with no vouchers
                User userWithNoVouchers = User.builder()
                                .name("User No Vouchers")
                                .email("novouchers@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.PREMIUM)
                                .build();
                userWithNoVouchers = userRepository.save(userWithNoVouchers);
                String token = jwtTokenProvider.generateAccessToken(
                                userWithNoVouchers.getId(), userWithNoVouchers.getEmail(),
                                userWithNoVouchers.getRole().name());

                // When & Then
                mockMvc.perform(get("/api/vouchers/my")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("POST /api/vouchers/redeem/{qrCode} - Should return 404 for non-existent voucher")
        void redeemVoucher_NotFound() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/vouchers/redeem/NON-EXISTENT-CODE")
                                .header("Authorization", "Bearer " + businessToken))
                                .andExpect(status().isNotFound());
        }
}
