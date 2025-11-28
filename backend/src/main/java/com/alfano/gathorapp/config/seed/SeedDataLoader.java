package com.alfano.gathorapp.config.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service responsible for loading seed data from YAML configuration file.
 *
 * This service reads the seed-data.yml file from the classpath and parses it
 * into a SeedData object containing all the seed entities.
 */
@Component
@Slf4j
public class SeedDataLoader {

    private static final String SEED_DATA_FILE = "seed-data.yml";

    /**
     * Load seed data from the YAML configuration file.
     *
     * @return SeedData object containing all seed entities
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public SeedData loadSeedData() {
        log.debug("Loading seed data from {}", SEED_DATA_FILE);

        try (InputStream inputStream = new ClassPathResource(SEED_DATA_FILE).getInputStream()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(SeedData.class, loaderOptions));
            SeedData seedData = yaml.load(inputStream);

            log.info("Successfully loaded seed data: {} users, {} events, {} rewards, {} outings",
                    seedData.getUsers() != null ? seedData.getUsers().size() : 0,
                    seedData.getEvents() != null ? seedData.getEvents().size() : 0,
                    seedData.getRewards() != null ? seedData.getRewards().size() : 0,
                    seedData.getOutings() != null ? seedData.getOutings().size() : 0);

            return seedData;

        } catch (IOException e) {
            log.error("Failed to load seed data from {}: {}", SEED_DATA_FILE, e.getMessage());
            throw new RuntimeException("Failed to load seed data configuration", e);
        }
    }

    /**
     * Load seed data from a custom file path (useful for testing).
     *
     * @param filePath path to the YAML file
     * @return SeedData object containing all seed entities
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public SeedData loadSeedData(String filePath) {
        log.debug("Loading seed data from custom path: {}", filePath);

        try (InputStream inputStream = new ClassPathResource(filePath).getInputStream()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(SeedData.class, loaderOptions));
            SeedData seedData = yaml.load(inputStream);

            log.info("Successfully loaded seed data from {}", filePath);
            return seedData;

        } catch (IOException e) {
            log.error("Failed to load seed data from {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Failed to load seed data from " + filePath, e);
        }
    }
}
