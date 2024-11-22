package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByExternalId(Long externalId);
}
