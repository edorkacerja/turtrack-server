package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, String> {
    Optional<DeliveryLocation> findByExternalId(String externalId);
}
