package com.alfano.gathorapp.config;

import com.alfano.gathorapp.config.seed.*;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DataSeeder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder Tests")
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private SeedDataLoader seedDataLoader;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Captor
    private ArgumentCaptor<Reward> rewardCaptor;

    @Captor
    private ArgumentCaptor<Outing> outingCaptor;

    private SeedData mockSeedData;

    @BeforeEach
    void setUp() {
        // Setup password encoder mock (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // Create mock seed data
        mockSeedData = createMockSeedData();
    }

    @Test
    @DisplayName("seedUsers - Should create users from seed data")
    void seedUsers_ValidData_CreatesUsers() {
        // Given
        List<UserSeed> userSeeds = mockSeedData.getUsers();

        // When
        Map<String, User> userMap = dataSeeder.seedUsers(userSeeds);

        // Then
        assertNotNull(userMap);
        assertEquals(2, userMap.size());
        verify(userRepository, times(2)).save(any(User.class));
        verify(passwordEncoder, times(2)).encode("password123");

        // Verify first user
        assertTrue(userMap.containsKey("user1@example.com"));
        User user1 = userMap.get("user1@example.com");
        assertEquals("User One", user1.getName());
        assertEquals("user1@example.com", user1.getEmail());
        assertEquals(Role.USER, user1.getRole());
    }

    @Test
    @DisplayName("seedUsers - Should handle empty list")
    void seedUsers_EmptyList_ReturnsEmptyMap() {
        // When
        Map<String, User> userMap = dataSeeder.seedUsers(new ArrayList<>());

        // Then
        assertNotNull(userMap);
        assertTrue(userMap.isEmpty());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("seedUsers - Should handle null list")
    void seedUsers_NullList_ReturnsEmptyMap() {
        // When
        Map<String, User> userMap = dataSeeder.seedUsers(null);

        // Then
        assertNotNull(userMap);
        assertTrue(userMap.isEmpty());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("seedEvents - Should create events from seed data")
    void seedEvents_ValidData_CreatesEvents() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        List<EventSeed> eventSeeds = mockSeedData.getEvents();

        // When
        Map<String, Event> eventMap = dataSeeder.seedEvents(eventSeeds, userMap);

        // Then
        assertNotNull(eventMap);
        assertEquals(1, eventMap.size());
        verify(eventRepository, times(1)).save(any(Event.class));

        // Verify event
        assertTrue(eventMap.containsKey("Test Event"));
        Event event = eventMap.get("Test Event");
        assertEquals("Test Event", event.getTitle());
        assertEquals("Test Description", event.getDescription());
        assertEquals("Test Location", event.getLocation());
        assertEquals(45.4642, event.getLatitude());
        assertEquals(9.1900, event.getLongitude());
    }

    @Test
    @DisplayName("seedEvents - Should skip events with non-existent creator")
    void seedEvents_NonExistentCreator_SkipsEvent() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        EventSeed eventWithInvalidCreator = new EventSeed();
        eventWithInvalidCreator.setTitle("Invalid Event");
        eventWithInvalidCreator.setCreatorEmail("nonexistent@example.com");
        List<EventSeed> eventSeeds = List.of(eventWithInvalidCreator);

        // When
        Map<String, Event> eventMap = dataSeeder.seedEvents(eventSeeds, userMap);

        // Then
        assertNotNull(eventMap);
        assertTrue(eventMap.isEmpty());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("seedEvents - Should handle empty list")
    void seedEvents_EmptyList_ReturnsEmptyMap() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());

        // When
        Map<String, Event> eventMap = dataSeeder.seedEvents(new ArrayList<>(), userMap);

        // Then
        assertNotNull(eventMap);
        assertTrue(eventMap.isEmpty());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("seedRewards - Should create rewards from seed data")
    void seedRewards_ValidData_CreatesRewards() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);
        List<RewardSeed> rewardSeeds = mockSeedData.getRewards();

        // When
        dataSeeder.seedRewards(rewardSeeds, eventMap, userMap);

        // Then
        verify(rewardRepository, times(1)).save(rewardCaptor.capture());
        Reward reward = rewardCaptor.getValue();
        assertEquals("Test Reward", reward.getTitle());
        assertEquals("Reward Description", reward.getDescription());
        assertEquals(5, reward.getRequiredParticipants());
    }

    @Test
    @DisplayName("seedRewards - Should skip rewards with non-existent event")
    void seedRewards_NonExistentEvent_SkipsReward() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);

        RewardSeed rewardWithInvalidEvent = new RewardSeed();
        rewardWithInvalidEvent.setTitle("Invalid Reward");
        rewardWithInvalidEvent.setEventTitle("NonExistent Event");
        rewardWithInvalidEvent.setBusinessEmail("user1@example.com");

        // When
        dataSeeder.seedRewards(List.of(rewardWithInvalidEvent), eventMap, userMap);

        // Then
        verify(rewardRepository, never()).save(any(Reward.class));
    }

    @Test
    @DisplayName("seedRewards - Should skip rewards with non-existent business")
    void seedRewards_NonExistentBusiness_SkipsReward() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);

        RewardSeed rewardWithInvalidBusiness = new RewardSeed();
        rewardWithInvalidBusiness.setTitle("Invalid Reward");
        rewardWithInvalidBusiness.setEventTitle("Test Event");
        rewardWithInvalidBusiness.setBusinessEmail("nonexistent@example.com");

        // When
        dataSeeder.seedRewards(List.of(rewardWithInvalidBusiness), eventMap, userMap);

        // Then
        verify(rewardRepository, never()).save(any(Reward.class));
    }

    @Test
    @DisplayName("seedOutings - Should create outings from seed data")
    void seedOutings_ValidData_CreatesOutings() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);
        List<OutingSeed> outingSeeds = mockSeedData.getOutings();

        // When
        dataSeeder.seedOutings(outingSeeds, userMap, eventMap);

        // Then
        verify(outingRepository, times(1)).save(outingCaptor.capture());
        Outing outing = outingCaptor.getValue();
        assertEquals("Test Outing", outing.getTitle());
        assertEquals("Outing Description", outing.getDescription());
        assertEquals("Outing Location", outing.getLocation());
        assertEquals(6, outing.getMaxParticipants());
    }

    @Test
    @DisplayName("seedOutings - Should create outings without events")
    void seedOutings_WithoutEvent_CreatesOutingWithNullEvent() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);

        OutingSeed independentOuting = new OutingSeed();
        independentOuting.setTitle("Independent Outing");
        independentOuting.setDescription("No event");
        independentOuting.setLocation("Somewhere");
        independentOuting.setLatitude(45.0);
        independentOuting.setLongitude(9.0);
        independentOuting.setDaysFromNow(5);
        independentOuting.setHour(10);
        independentOuting.setMinute(0);
        independentOuting.setMaxParticipants(10);
        independentOuting.setOrganizerEmail("user1@example.com");
        independentOuting.setEventTitle(null);

        // When
        dataSeeder.seedOutings(List.of(independentOuting), userMap, eventMap);

        // Then
        verify(outingRepository, times(1)).save(outingCaptor.capture());
        Outing outing = outingCaptor.getValue();
        assertNull(outing.getEvent());
    }

    @Test
    @DisplayName("seedOutings - Should skip outings with non-existent organizer")
    void seedOutings_NonExistentOrganizer_SkipsOuting() {
        // Given
        Map<String, User> userMap = dataSeeder.seedUsers(mockSeedData.getUsers());
        Map<String, Event> eventMap = dataSeeder.seedEvents(mockSeedData.getEvents(), userMap);

        OutingSeed outingWithInvalidOrganizer = new OutingSeed();
        outingWithInvalidOrganizer.setTitle("Invalid Outing");
        outingWithInvalidOrganizer.setOrganizerEmail("nonexistent@example.com");

        // When
        dataSeeder.seedOutings(List.of(outingWithInvalidOrganizer), userMap, eventMap);

        // Then
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    @DisplayName("seedDatabase - Should seed all entities in correct order")
    void seedDatabase_FullData_SeedsAllEntities() {
        // When
        dataSeeder.seedDatabase(mockSeedData);

        // Then
        // Verify all repositories were called
        verify(userRepository, times(2)).save(any(User.class));
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(rewardRepository, times(1)).save(any(Reward.class));
        verify(outingRepository, times(1)).save(any(Outing.class));
    }

    @Test
    @DisplayName("seedDatabase - Should handle empty seed data gracefully")
    void seedDatabase_EmptyData_HandlesGracefully() {
        // Given
        SeedData emptySeedData = new SeedData();
        emptySeedData.setUsers(new ArrayList<>());
        emptySeedData.setEvents(new ArrayList<>());
        emptySeedData.setRewards(new ArrayList<>());
        emptySeedData.setOutings(new ArrayList<>());

        // When
        dataSeeder.seedDatabase(emptySeedData);

        // Then
        verify(userRepository, never()).save(any(User.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(rewardRepository, never()).save(any(Reward.class));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    @Test
    @DisplayName("seedDatabase - Should handle null collections in seed data")
    void seedDatabase_NullCollections_HandlesGracefully() {
        // Given
        SeedData seedDataWithNulls = new SeedData();
        seedDataWithNulls.setUsers(null);
        seedDataWithNulls.setEvents(null);
        seedDataWithNulls.setRewards(null);
        seedDataWithNulls.setOutings(null);

        // When
        dataSeeder.seedDatabase(seedDataWithNulls);

        // Then
        verify(userRepository, never()).save(any(User.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(rewardRepository, never()).save(any(Reward.class));
        verify(outingRepository, never()).save(any(Outing.class));
    }

    // Helper method to create mock seed data
    private SeedData createMockSeedData() {
        SeedData seedData = new SeedData();

        // Create user seeds
        List<UserSeed> userSeeds = new ArrayList<>();
        UserSeed user1 = new UserSeed();
        user1.setName("User One");
        user1.setEmail("user1@example.com");
        user1.setPassword("password123");
        user1.setRole("USER");
        userSeeds.add(user1);

        UserSeed user2 = new UserSeed();
        user2.setName("User Two");
        user2.setEmail("user2@example.com");
        user2.setPassword("password123");
        user2.setRole("PREMIUM");
        userSeeds.add(user2);

        seedData.setUsers(userSeeds);

        // Create event seeds
        List<EventSeed> eventSeeds = new ArrayList<>();
        EventSeed event = new EventSeed();
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setLocation("Test Location");
        event.setLatitude(45.4642);
        event.setLongitude(9.1900);
        event.setDaysFromNow(5);
        event.setHour(19);
        event.setMinute(0);
        event.setCreatorEmail("user1@example.com");
        eventSeeds.add(event);

        seedData.setEvents(eventSeeds);

        // Create reward seeds
        List<RewardSeed> rewardSeeds = new ArrayList<>();
        RewardSeed reward = new RewardSeed();
        reward.setTitle("Test Reward");
        reward.setDescription("Reward Description");
        reward.setRequiredParticipants(5);
        reward.setEventTitle("Test Event");
        reward.setBusinessEmail("user1@example.com");
        rewardSeeds.add(reward);

        seedData.setRewards(rewardSeeds);

        // Create outing seeds
        List<OutingSeed> outingSeeds = new ArrayList<>();
        OutingSeed outing = new OutingSeed();
        outing.setTitle("Test Outing");
        outing.setDescription("Outing Description");
        outing.setLocation("Outing Location");
        outing.setLatitude(45.5);
        outing.setLongitude(9.2);
        outing.setDaysFromNow(3);
        outing.setHour(18);
        outing.setMinute(30);
        outing.setMaxParticipants(6);
        outing.setOrganizerEmail("user2@example.com");
        outing.setEventTitle("Test Event");
        outingSeeds.add(outing);

        seedData.setOutings(outingSeeds);

        return seedData;
    }
}
