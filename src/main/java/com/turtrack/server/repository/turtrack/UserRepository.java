package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStripeCustomerId(String stripeCustomerId);
    boolean existsByEmail(String email);
}