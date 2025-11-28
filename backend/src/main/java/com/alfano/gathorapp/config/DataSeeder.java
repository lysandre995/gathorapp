package com.alfano.gathorapp.config;

import com.alfano.gathorapp.config.seed.*;
import com.alfano.gathorapp.event.Event;
import com.alfano.gathorapp.event.EventRepository;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.reward.RewardRepository;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Populate the DB with test data at application startup.
 *
 * Loads seed data from external YAML configuration file (seed-data.yml)
 * for cleaner and more maintainable seed logic.
 *
 * @author Alessandro Alfano
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventRepository eventRepository;
    private final OutingRepository outingRepository;
    private final RewardRepository rewardRepository;
    private final SeedDataLoader seedDataLoader;

    /**
     * Initialize database with seed data from configuration file.
     */
    @Bean
    @ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("Seed data initialization...");

            // Skip if data already exists
            if (userRepository.count() > 0) {
                log.info("DB already filled, skip seed.");
                return;
            }

            try {
                SeedData seedData = seedDataLoader.loadSeedData();
                seedDatabase(seedData);
            } catch (Exception e) {
                log.error("Failed to seed database: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    /**
     * Seed all entities from the loaded seed data.
     *
     * @param seedData the seed data loaded from configuration
     */
    void seedDatabase(SeedData seedData) {
        Map<String, User> userMap = seedUsers(seedData.getUsers());
        Map<String, Event> eventMap = seedEvents(seedData.getEvents(), userMap);
        seedRewards(seedData.getRewards(), eventMap, userMap);
        seedOutings(seedData.getOutings(), userMap, eventMap);

        log.info("Seed completed! DB ready with:");
        log.info("   - {} users", userMap.size());
        log.info("   - {} events", eventMap.size());
        log.info("   - {} rewards", seedData.getRewards() != null ? seedData.getRewards().size() : 0);
        log.info("   - {} outings", seedData.getOutings() != null ? seedData.getOutings().size() : 0);
    }

    /**
     * Seed users from configuration.
     *
     * @param userSeeds list of user seed data
     * @return map of email to User entity
     */
    Map<String, User> seedUsers(java.util.List<UserSeed> userSeeds) {
        Map<String, User> userMap = new HashMap<>();

        if (userSeeds == null || userSeeds.isEmpty()) {
            log.warn("No users to seed");
            return userMap;
        }

        for (UserSeed userSeed : userSeeds) {
            User user = User.builder()
                    .name(userSeed.getName())
                    .email(userSeed.getEmail())
                    .passwordHash(passwordEncoder.encode(userSeed.getPassword()))
                    .role(Role.valueOf(userSeed.getRole()))
                    .build();
            userRepository.save(user);
            userMap.put(user.getEmail(), user);
            log.info("{} user created: {} / {}", userSeed.getRole(), userSeed.getEmail(), userSeed.getPassword());
        }

        return userMap;
    }

    /**
     * Seed events from configuration.
     *
     * @param eventSeeds list of event seed data
     * @param userMap    map of email to User entity
     * @return map of event title to Event entity
     */
    Map<String, Event> seedEvents(java.util.List<EventSeed> eventSeeds, Map<String, User> userMap) {
        Map<String, Event> eventMap = new HashMap<>();

        if (eventSeeds == null || eventSeeds.isEmpty()) {
            log.warn("No events to seed");
            return eventMap;
        }

        for (EventSeed eventSeed : eventSeeds) {
            User creator = userMap.get(eventSeed.getCreatorEmail());
            if (creator == null) {
                log.error("Creator not found for event: {}", eventSeed.getTitle());
                continue;
            }

            Event event = Event.builder()
                    .title(eventSeed.getTitle())
                    .description(eventSeed.getDescription())
                    .location(eventSeed.getLocation())
                    .latitude(eventSeed.getLatitude())
                    .longitude(eventSeed.getLongitude())
                    .eventDate(LocalDateTime.now()
                            .plusDays(eventSeed.getDaysFromNow())
                            .withHour(eventSeed.getHour())
                            .withMinute(eventSeed.getMinute()))
                    .creator(creator)
                    .build();
            eventRepository.save(event);
            eventMap.put(event.getTitle(), event);
            log.info("Event created: {}", eventSeed.getTitle());
        }

        return eventMap;
    }

    /**
     * Seed rewards from configuration.
     *
     * @param rewardSeeds list of reward seed data
     * @param eventMap    map of event title to Event entity
     * @param userMap     map of email to User entity
     */
    void seedRewards(java.util.List<RewardSeed> rewardSeeds, Map<String, Event> eventMap, Map<String, User> userMap) {
        if (rewardSeeds == null || rewardSeeds.isEmpty()) {
            log.warn("No rewards to seed");
            return;
        }

        for (RewardSeed rewardSeed : rewardSeeds) {
            Event event = eventMap.get(rewardSeed.getEventTitle());
            User business = userMap.get(rewardSeed.getBusinessEmail());

            if (event == null) {
                log.error("Event not found for reward: {}", rewardSeed.getTitle());
                continue;
            }
            if (business == null) {
                log.error("Business not found for reward: {}", rewardSeed.getTitle());
                continue;
            }

            Reward reward = Reward.builder()
                    .title(rewardSeed.getTitle())
                    .description(rewardSeed.getDescription())
                    .requiredParticipants(rewardSeed.getRequiredParticipants())
                    .event(event)
                    .business(business)
                    .build();
            rewardRepository.save(reward);
            log.info("Reward created: {}", rewardSeed.getTitle());
        }
    }

    /**
     * Seed outings from configuration.
     *
     * @param outingSeeds list of outing seed data
     * @param userMap     map of email to User entity
     * @param eventMap    map of event title to Event entity
     */
    void seedOutings(java.util.List<OutingSeed> outingSeeds, Map<String, User> userMap, Map<String, Event> eventMap) {
        if (outingSeeds == null || outingSeeds.isEmpty()) {
            log.warn("No outings to seed");
            return;
        }

        for (OutingSeed outingSeed : outingSeeds) {
            User organizer = userMap.get(outingSeed.getOrganizerEmail());
            if (organizer == null) {
                log.error("Organizer not found for outing: {}", outingSeed.getTitle());
                continue;
            }

            Event event = null;
            if (outingSeed.getEventTitle() != null) {
                event = eventMap.get(outingSeed.getEventTitle());
            }

            Outing outing = Outing.builder()
                    .title(outingSeed.getTitle())
                    .description(outingSeed.getDescription())
                    .location(outingSeed.getLocation())
                    .latitude(outingSeed.getLatitude())
                    .longitude(outingSeed.getLongitude())
                    .outingDate(LocalDateTime.now()
                            .plusDays(outingSeed.getDaysFromNow())
                            .withHour(outingSeed.getHour())
                            .withMinute(outingSeed.getMinute()))
                    .maxParticipants(outingSeed.getMaxParticipants())
                    .organizer(organizer)
                    .event(event)
                    .build();
            outingRepository.save(outing);
            log.info("Outing created: {}", outingSeed.getTitle());
        }
    }
}
