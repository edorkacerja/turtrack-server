package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByExternalId(Long externalId);
}
