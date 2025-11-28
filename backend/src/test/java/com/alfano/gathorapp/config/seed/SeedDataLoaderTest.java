package com.alfano.gathorapp.config.seed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeedDataLoader.
 */
@DisplayName("SeedDataLoader Tests")
class SeedDataLoaderTest {

    private SeedDataLoader seedDataLoader;

    @BeforeEach
    void setUp() {
        seedDataLoader = new SeedDataLoader();
    }

    @Test
    @DisplayName("loadSeedData - Should load default seed data successfully")
    void loadSeedData_DefaultFile_LoadsSuccessfully() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        assertNotNull(seedData);
        assertNotNull(seedData.getUsers());
        assertNotNull(seedData.getEvents());
        assertNotNull(seedData.getRewards());
        assertNotNull(seedData.getOutings());

        // Verify expected counts from seed-data.yml
        assertEquals(5, seedData.getUsers().size());
        assertEquals(4, seedData.getEvents().size());
        assertEquals(2, seedData.getRewards().size());
        assertEquals(4, seedData.getOutings().size());
    }

    @Test
    @DisplayName("loadSeedData - Should load user data correctly")
    void loadSeedData_UsersData_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        UserSeed firstUser = seedData.getUsers().get(0);
        assertNotNull(firstUser);
        assertEquals("Mario Rossi", firstUser.getName());
        assertEquals("mario@example.com", firstUser.getEmail());
        assertEquals("password123", firstUser.getPassword());
        assertEquals("USER", firstUser.getRole());
    }

    @Test
    @DisplayName("loadSeedData - Should load event data correctly")
    void loadSeedData_EventsData_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        EventSeed firstEvent = seedData.getEvents().get(0);
        assertNotNull(firstEvent);
        assertEquals("Pizza Night - Aperitivo with Live Music", firstEvent.getTitle());
        assertNotNull(firstEvent.getDescription());
        assertEquals("Piazza Sant'Oronzo, Lecce", firstEvent.getLocation());
        assertEquals(40.3515, firstEvent.getLatitude());
        assertEquals(18.1750, firstEvent.getLongitude());
        assertEquals(3, firstEvent.getDaysFromNow());
        assertEquals(19, firstEvent.getHour());
        assertEquals(0, firstEvent.getMinute());
        assertEquals("antonio@pizzeria.com", firstEvent.getCreatorEmail());
    }

    @Test
    @DisplayName("loadSeedData - Should load reward data correctly")
    void loadSeedData_RewardsData_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        RewardSeed firstReward = seedData.getRewards().get(0);
        assertNotNull(firstReward);
        assertEquals("Free Pizza Margherita", firstReward.getTitle());
        assertNotNull(firstReward.getDescription());
        assertEquals(5, firstReward.getRequiredParticipants());
        assertEquals("Pizza Night - Aperitivo with Live Music", firstReward.getEventTitle());
        assertEquals("antonio@pizzeria.com", firstReward.getBusinessEmail());
    }

    @Test
    @DisplayName("loadSeedData - Should load outing data correctly")
    void loadSeedData_OutingsData_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        OutingSeed firstOuting = seedData.getOutings().get(0);
        assertNotNull(firstOuting);
        assertEquals("Pizza Night Group - University Students", firstOuting.getTitle());
        assertNotNull(firstOuting.getDescription());
        assertEquals("Piazza Sant'Oronzo, Lecce", firstOuting.getLocation());
        assertEquals(40.3515, firstOuting.getLatitude());
        assertEquals(18.1750, firstOuting.getLongitude());
        assertEquals(3, firstOuting.getDaysFromNow());
        assertEquals(18, firstOuting.getHour());
        assertEquals(45, firstOuting.getMinute());
        assertEquals(6, firstOuting.getMaxParticipants());
        assertEquals("laura@example.com", firstOuting.getOrganizerEmail());
        assertEquals("Pizza Night - Aperitivo with Live Music", firstOuting.getEventTitle());
    }

    @Test
    @DisplayName("loadSeedData - Should load all user roles")
    void loadSeedData_AllUserRoles_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        assertEquals(5, seedData.getUsers().size());

        // Verify all roles are present
        assertTrue(seedData.getUsers().stream().anyMatch(u -> "USER".equals(u.getRole())));
        assertTrue(seedData.getUsers().stream().anyMatch(u -> "PREMIUM".equals(u.getRole())));
        assertTrue(seedData.getUsers().stream().anyMatch(u -> "BUSINESS".equals(u.getRole())));
        assertTrue(seedData.getUsers().stream().anyMatch(u -> "ADMIN".equals(u.getRole())));
    }

    @Test
    @DisplayName("loadSeedData - Should load outings with and without events")
    void loadSeedData_OutingsWithAndWithoutEvents_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        // Some outings should have eventTitle
        assertTrue(seedData.getOutings().stream().anyMatch(o -> o.getEventTitle() != null));

        // Some outings should not have eventTitle (independent outings)
        assertTrue(seedData.getOutings().stream().anyMatch(o -> o.getEventTitle() == null));
    }

    @Test
    @DisplayName("loadSeedData - Should throw exception for non-existent file")
    void loadSeedData_NonExistentFile_ThrowsException() {
        // When / Then
        assertThrows(RuntimeException.class, () -> {
            seedDataLoader.loadSeedData("non-existent-file.yml");
        });
    }

    @Test
    @DisplayName("loadSeedData - Should load from custom path")
    void loadSeedData_CustomPath_LoadsSuccessfully() {
        // When - Load the same file but using the custom path method
        SeedData seedData = seedDataLoader.loadSeedData("seed-data.yml");

        // Then
        assertNotNull(seedData);
        assertNotNull(seedData.getUsers());
        assertEquals(5, seedData.getUsers().size());
    }

    @Test
    @DisplayName("loadSeedData - Should handle multiple events correctly")
    void loadSeedData_MultipleEvents_LoadsAllCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        assertEquals(4, seedData.getEvents().size());

        // Verify all events have required fields
        for (EventSeed event : seedData.getEvents()) {
            assertNotNull(event.getTitle());
            assertNotNull(event.getDescription());
            assertNotNull(event.getLocation());
            assertNotNull(event.getLatitude());
            assertNotNull(event.getLongitude());
            assertNotNull(event.getDaysFromNow());
            assertNotNull(event.getHour());
            assertNotNull(event.getMinute());
            assertNotNull(event.getCreatorEmail());
        }
    }

    @Test
    @DisplayName("loadSeedData - Should load business users correctly")
    void loadSeedData_BusinessUsers_LoadsCorrectly() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        long businessCount = seedData.getUsers().stream()
                .filter(u -> "BUSINESS".equals(u.getRole()))
                .count();

        assertEquals(2, businessCount);

        // Verify specific business users
        assertTrue(seedData.getUsers().stream()
                .anyMatch(u -> "antonio@pizzeria.com".equals(u.getEmail())));
        assertTrue(seedData.getUsers().stream()
                .anyMatch(u -> "irish@pub.com".equals(u.getEmail())));
    }

    @Test
    @DisplayName("loadSeedData - Should load coordinates as Double type")
    void loadSeedData_Coordinates_LoadsAsDoubleType() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        for (EventSeed event : seedData.getEvents()) {
            assertNotNull(event.getLatitude());
            assertNotNull(event.getLongitude());
            assertTrue(event.getLatitude() instanceof Double);
            assertTrue(event.getLongitude() instanceof Double);
        }

        for (OutingSeed outing : seedData.getOutings()) {
            assertNotNull(outing.getLatitude());
            assertNotNull(outing.getLongitude());
            assertTrue(outing.getLatitude() instanceof Double);
            assertTrue(outing.getLongitude() instanceof Double);
        }
    }

    @Test
    @DisplayName("loadSeedData - Should load time fields as Integer type")
    void loadSeedData_TimeFields_LoadsAsIntegerType() {
        // When
        SeedData seedData = seedDataLoader.loadSeedData();

        // Then
        for (EventSeed event : seedData.getEvents()) {
            assertNotNull(event.getDaysFromNow());
            assertNotNull(event.getHour());
            assertNotNull(event.getMinute());
            assertTrue(event.getDaysFromNow() instanceof Integer);
            assertTrue(event.getHour() instanceof Integer);
            assertTrue(event.getMinute() instanceof Integer);
        }
    }
}
