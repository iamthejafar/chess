package com.jafar.chess.repository;

import com.jafar.chess.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email (for Google users)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Google ID
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}