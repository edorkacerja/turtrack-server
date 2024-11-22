package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.VehicleDeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDeliveryLocationRepository extends JpaRepository<VehicleDeliveryLocation, Long> {
}
