package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByExternalId(Long externalId);
}
