package com.turtrack.server.repository.manager;

import com.turtrack.server.model.manager.Job;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
@Profile("dev")
public interface JobRepository extends JpaRepository<Job, Long> {
}
