package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.DailyRateAndAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyRateAndAvailabilityRepository extends JpaRepository<DailyRateAndAvailability, Long> {
}
