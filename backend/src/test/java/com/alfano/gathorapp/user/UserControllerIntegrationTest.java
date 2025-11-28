package com.alfano.gathorapp.user;

import com.alfano.gathorapp.auth.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests user CRUD operations and profile management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private com.alfano.gathorapp.testutils.TestDatabaseCleaner testDatabaseCleaner;

        private User testUser;
        private User adminUser;
        private String userToken;

        @BeforeEach
        void setUp() {
                // Ensure a clean database state for each test
                testDatabaseCleaner.truncateAll();

                // Create test user
                testUser = User.builder()
                                .name("Test User")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.USER)
                                .build();
                testUser = userRepository.save(testUser);

                // Create admin user
                adminUser = User.builder()
                                .name("Admin User")
                                .email("admin@example.com")
                                .passwordHash("hashedPassword")
                                .role(Role.ADMIN)
                                .build();
                adminUser = userRepository.save(adminUser);

                // Generate tokens
                userToken = jwtTokenProvider.generateAccessToken(
                                testUser.getId(), testUser.getEmail(), testUser.getRole().name());
        }

        @Test
        @DisplayName("GET /api/users - Should return all users")
        void getAllUsers_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[*].email", hasItem("test@example.com")))
                                .andExpect(jsonPath("$[*].email", hasItem("admin@example.com")));
        }

        @Test
        @DisplayName("GET /api/users/{id} - Should return user by id")
        void getUserById_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users/" + testUser.getId())
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(testUser.getId().toString())))
                                .andExpect(jsonPath("$.name", is("Test User")))
                                .andExpect(jsonPath("$.email", is("test@example.com")))
                                .andExpect(jsonPath("$.role", is("USER")));
        }

        @Test
        @DisplayName("GET /api/users/{id} - Should return 404 for non-existent user")
        void getUserById_NotFound() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isNotFound());
        }
}
