package com.turtrack.server.repository.manager;

import com.turtrack.server.model.manager.OptimalCell;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@Profile("dev")
public interface OptimalCellRepository extends JpaRepository<OptimalCell, UUID> {
    List<OptimalCell> findByCellSize(int cellSize);
}
