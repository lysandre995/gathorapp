package com.alfano.gathorapp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository per l'entità User.
 * Spring Data JPA genera automaticamente l'implementazione.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Trova un utente per email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se esiste un utente con una data email.
     */
    boolean existsByEmail(String email);

    /**
     * Count users by role.
     */
    long countByRole(Role role);

    /**
     * Find all users by role.
     */
    List<User> findByRole(Role role);
}
