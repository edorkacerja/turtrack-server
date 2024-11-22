package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
