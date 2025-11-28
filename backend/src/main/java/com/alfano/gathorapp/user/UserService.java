package com.alfano.gathorapp.user;

import com.alfano.gathorapp.user.dto.CreateUserRequest;
import com.alfano.gathorapp.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alfano.gathorapp.user.dto.UpdateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Recupero tutti gli utenti");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("Recupero utente con id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.alfano.gathorapp.exception.ResourceNotFoundException(
                        "Utente non trovato con id: " + id));
        return userMapper.toResponse(user);
    }

    /**
     * Create new user.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creazione nuovo utente: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email già in uso: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("Utente creato con successo: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    /**
     * Delete a user by ID.
     */
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Eliminazione utente con id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utente non trovato con id: " + id);
        }
        userRepository.deleteById(id);
        log.info("Utente eliminato con successo: {}", id);
    }

    /**
     * Update user profile.
     * Only updates fields that are provided (not null).
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        boolean updated = false;

        // Update name if provided
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
            updated = true;
        }

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check if email is already in use by another user
            if (!user.getEmail().equals(request.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            updated = true;
        }

        // Update password if provided
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            updated = true;
        }

        if (updated) {
            User updatedUser = userRepository.save(user);
            log.info("User updated successfully: {}", userId);
            return userMapper.toResponse(updatedUser);
        } else {
            log.debug("No fields to update for user: {}", userId);
            return userMapper.toResponse(user);
        }
    }

    /**
     * Change user account role (upgrade/downgrade).
     * This is a test/demo feature for university project.
     * In production, this would be behind a payment flow.
     *
     * Allowed transitions:
     * - USER → PREMIUM or BUSINESS
     * - PREMIUM → USER or BUSINESS
     * - BUSINESS → USER or PREMIUM
     *
     * Admin/Maintainer roles cannot be changed through this endpoint.
     */
    @Transactional
    public UserResponse upgradeUserAccount(UUID userId, Role newRole) {
        log.info("Changing user {} role to {}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Role currentRole = user.getRole();

        // Prevent changing admin roles
        if (currentRole == Role.ADMIN) {
            throw new RuntimeException("Cannot change ADMIN role through this endpoint");
        }

        // Validate target role (only USER, PREMIUM, BUSINESS allowed)
        if (newRole != Role.USER && newRole != Role.PREMIUM && newRole != Role.BUSINESS) {
            throw new RuntimeException("Invalid role. Only USER, PREMIUM and BUSINESS are allowed.");
        }

        // Check if already at target role
        if (currentRole == newRole) {
            throw new RuntimeException("User already has role: " + newRole);
        }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        String action = isUpgrade(currentRole, newRole) ? "upgraded" : "downgraded";
        log.info("User {} successfully {} to {}", userId, action, newRole);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Determine if role change is an upgrade or downgrade.
     */
    private boolean isUpgrade(Role from, Role to) {
        int fromLevel = getRoleLevel(from);
        int toLevel = getRoleLevel(to);
        return toLevel > fromLevel;
    }

    /**
     * Get role hierarchy level for comparison.
     */
    private int getRoleLevel(Role role) {
        switch (role) {
            case USER:
                return 1;
            case PREMIUM:
                return 2;
            case BUSINESS:
                return 3;
            default:
                return 0;
        }
    }
}
