package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Optional<Badge> findByExternalId(Long externalId);
}
