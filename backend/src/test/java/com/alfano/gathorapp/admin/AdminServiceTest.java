package com.alfano.gathorapp.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private OutingRepository outingRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatDeactivationScheduler chatDeactivationScheduler;
    @Mock
    private VoucherService voucherService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        this.adminService = new AdminService(userRepository, eventRepository, outingRepository,
                participationRepository, chatRepository, voucherRepository, userMapper, chatDeactivationScheduler,
                voucherService);
    }

    @Test
    void test_getAllUsers() {
        UUID id1 = UUID.randomUUID();
        User user1 = new User(
                id1,
                "Pippo",
                "pippo@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(2000, 11, 20, 10, 30));
        UUID id2 = UUID.randomUUID();
        User user2 = new User(
                id2,
                "Pluto",
                "pluto@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(1995, 8, 15, 20, 15));
        List<User> result = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(result);

        UserResponse userResponse1 = new UserResponse(
                id1,
                user1.getName(),
                user1.getEmail(),
                user1.getRole(),
                user1.getCreatedAt());
        when(userMapper.toResponse(user1)).thenReturn(userResponse1);

        UserResponse userResponse2 = new UserResponse(
                id2,
                user2.getName(),
                user2.getEmail(),
                user2.getRole(),
                user2.getCreatedAt());
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);

        List<UserResponse> actual = adminService.getAllUsers();

        verify(userRepository).findAll();
        verify(userMapper).toResponse(user1);
        verify(userMapper).toResponse(user2);

        assertEquals(id1, actual.get(0).getId());
        assertEquals(user1.getName(), actual.get(0).getName());
        assertEquals(user1.getEmail(), actual.get(0).getEmail());
        assertEquals(user1.getRole(), actual.get(0).getRole());
        assertEquals(user1.getCreatedAt(), actual.get(0).getCreatedAt());

        assertEquals(id2, actual.get(1).getId());
        assertEquals(user2.getName(), actual.get(1).getName());
        assertEquals(user2.getEmail(), actual.get(1).getEmail());
        assertEquals(user2.getRole(), actual.get(1).getRole());
        assertEquals(user2.getCreatedAt(), actual.get(1).getCreatedAt());
    }

    @Test
    void test_changeUserRole_userExists() {
        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                "Pippo",
                "pippo@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(2000, 11, 20, 10, 30));

        UserResponse userResponse = new UserResponse(
                id,
                user.getName(),
                user.getEmail(),
                Role.ADMIN,
                user.getCreatedAt());

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse actual = adminService.changeUserRole(id, Role.ADMIN);

        verify(userRepository).findById(id);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);

        assertEquals(userResponse.getId(), actual.getId());
        assertEquals(userResponse.getName(), actual.getName());
        assertEquals(userResponse.getEmail(), actual.getEmail());
        assertEquals(Role.ADMIN, actual.getRole());
        assertEquals(userResponse.getCreatedAt(), actual.getCreatedAt());
    }

    @Test
    void test_changeUserRole_userDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Verify a RuntimeException is thrown
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.changeUserRole(id, Role.ADMIN));

        assertEquals("User not found with id: " + id, ex.getMessage());

        verify(userRepository).findById(id);
    }

    @Test
    void test_deleteUser_userExists() {
        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                "Pippo",
                "pippo@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(2000, 11, 20, 10, 30));

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        adminService.deleteUser(id);

        verify(userRepository).findById(id);
        verify(userRepository).delete(user);
    }

    @Test
    void test_deleteUser_userDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Verify a RuntimeException is thrown
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.deleteUser(id));

        assertEquals("User not found with id: " + id, ex.getMessage());

        verify(userRepository).findById(id);
    }

    @Test
    void test_banUser_userExists() {
        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                "Pippo",
                "pippo@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(2000, 11, 20, 10, 30));

        UserResponse userResponse = new UserResponse(
                id,
                user.getName(),
                user.getEmail(),
                Role.ADMIN,
                user.getCreatedAt());

        String reason = "Bad user";

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse actual = adminService.banUser(id, reason);

        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);

        assertEquals(userResponse.getId(), actual.getId());
        assertEquals(userResponse.getName(), actual.getName());
        assertEquals(userResponse.getEmail(), actual.getEmail());
        assertEquals(userResponse.getRole(), actual.getRole());
        assertEquals(userResponse.getCreatedAt(), actual.getCreatedAt());
    }

    @Test
    void test_banUser_userDoesNotExist() {
        UUID id = UUID.randomUUID();

        String reason = "Bad user";

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.banUser(id, reason));

        assertEquals("User not found with id: " + id, ex.getMessage());

        verify(userRepository).findById(id);
    }

    @Test
    void test_unbanUser_userExists() {
        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                "Pippo",
                "pippo@example.com",
                "password123",
                Role.USER,
                false,
                LocalDateTime.of(2000, 11, 20, 10, 30));

        UserResponse userResponse = new UserResponse(
                id,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt());

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse actual = adminService.unbanUser(id);

        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);

        assertEquals(userResponse.getId(), actual.getId());
        assertEquals(userResponse.getName(), actual.getName());
    }

    @Test
    void test_unbanUser_userDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.unbanUser(id));

        assertEquals("User not found with id: " + id, ex.getMessage());

        verify(userRepository).findById(id);
    }

    @Test
    void test_getStatistics_success() {
        // Mock user statistics
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(Role.USER)).thenReturn(70L);
        when(userRepository.countByRole(Role.PREMIUM)).thenReturn(20L);
        when(userRepository.countByRole(Role.BUSINESS)).thenReturn(10L);

        // Mock event statistics
        when(eventRepository.count()).thenReturn(50L);
        when(eventRepository.findUpcomingEvents(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Mock outing statistics
        when(outingRepository.count()).thenReturn(200L);
        when(outingRepository.findUpcomingOutings(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Mock participation statistics
        when(participationRepository.count()).thenReturn(500L);

        // Mock chat statistics
        when(chatRepository.count()).thenReturn(150L);
        when(chatRepository.findByActiveTrue()).thenReturn(List.of());

        // Mock voucher statistics
        when(voucherRepository.count()).thenReturn(30L);
        when(voucherRepository.countAllActiveVouchers(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(15L);

        java.util.Map<String, Object> stats = adminService.getStatistics();

        // Verify user statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> userStats = (java.util.Map<String, Object>) stats.get("users");
        assertEquals(100L, userStats.get("total"));
        assertEquals(70L, userStats.get("base"));
        assertEquals(20L, userStats.get("premium"));
        assertEquals(10L, userStats.get("business"));

        // Verify event statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> eventStats = (java.util.Map<String, Object>) stats.get("events");
        assertEquals(50L, eventStats.get("total"));
        assertEquals(0L, eventStats.get("upcoming"));

        // Verify outing statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> outingStats = (java.util.Map<String, Object>) stats.get("outings");
        assertEquals(200L, outingStats.get("total"));
        assertEquals(0L, outingStats.get("upcoming"));

        // Verify participation statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> participationStats = (java.util.Map<String, Object>) stats.get("participations");
        assertEquals(500L, participationStats.get("total"));

        // Verify chat statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> chatStats = (java.util.Map<String, Object>) stats.get("chats");
        assertEquals(150L, chatStats.get("total"));
        assertEquals(0L, chatStats.get("active"));

        // Verify voucher statistics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> voucherStats = (java.util.Map<String, Object>) stats.get("vouchers");
        assertEquals(30L, voucherStats.get("total"));
        assertEquals(15L, voucherStats.get("active"));

        // Verify all repository methods were called
        verify(userRepository).count();
        verify(userRepository).countByRole(Role.USER);
        verify(userRepository).countByRole(Role.PREMIUM);
        verify(userRepository).countByRole(Role.BUSINESS);
        verify(eventRepository).count();
        verify(outingRepository).count();
        verify(participationRepository).count();
        verify(chatRepository).count();
        verify(voucherRepository).count();
    }

    @Test
    void test_cleanupExpiredChats() {
        doNothing().when(chatDeactivationScheduler).deactivateExpiredChats();

        int result = adminService.cleanupExpiredChats();

        verify(chatDeactivationScheduler).deactivateExpiredChats();
        assertEquals(0, result); // Placeholder return value
    }

    @Test
    void test_cleanupExpiredVouchers() {
        doNothing().when(voucherService).expireOldVouchers();

        int result = adminService.cleanupExpiredVouchers();

        verify(voucherService).expireOldVouchers();
        assertEquals(0, result); // Placeholder return value
    }
}
