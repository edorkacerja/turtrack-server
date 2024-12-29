package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.DailyRateAndAvailability;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("dev")
public interface DailyRateAndAvailabilityRepository extends JpaRepository<DailyRateAndAvailability, Long> {
}
