package com.turtrack.server.dto.manager;

import com.turtrack.server.model.manager.Job;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAvailabilityJobDTO {
    private Job.JobType jobType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfVehicles;
}