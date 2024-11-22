package com.turtrack.server.repository.turtrack;


import com.turtrack.server.model.turtrack.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByExternalId(Long externalId);
}
