package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Extra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExtraRepository extends JpaRepository<Extra, Long> {
    Optional<Extra> findByExternalId(Long externalId);
}
