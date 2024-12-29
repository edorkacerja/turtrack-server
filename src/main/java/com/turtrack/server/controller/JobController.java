package com.turtrack.server.controller;

import com.turtrack.server.dto.manager.CreateSearchJobDTO;
import com.turtrack.server.dto.manager.CreateAvailabilityJobDTO;
import com.turtrack.server.dto.manager.CreateVehicleDetailsJobDTO;
import com.turtrack.server.dto.manager.JobDTO;
import com.turtrack.server.model.manager.Job;
import com.turtrack.server.service.manager.DailyRateAndAvailabilityJobService;
import com.turtrack.server.service.manager.JobService;
import com.turtrack.server.service.manager.SearchJobService;
import com.turtrack.server.service.manager.VehicleDetailsJobService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/jobs")
@Slf4j
@AllArgsConstructor
@Profile("dev")
public class JobController {

    private final JobService jobService;
    private final DailyRateAndAvailabilityJobService availabilityJobService;
    private final SearchJobService searchJobService;
    private final VehicleDetailsJobService vehicleDetailsJobService;

    @GetMapping
    public ResponseEntity<Page<JobDTO>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startedAt,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(createSortOrder(sort)));
        Page<Job> jobPage = jobService.getAllJobs(pageable);
        Page<JobDTO> jobDTOPage = jobPage.map(JobDTO::toDTO);
        return ResponseEntity.ok(jobDTOPage);
    }

    private Sort.Order createSortOrder(String[] sort) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (sort[1].equals("desc")) {
            direction = Sort.Direction.DESC;
        }
        return new Sort.Order(direction, sort[0]);
    }

//    @PostMapping("/create")
//    public ResponseEntity<JobDTO> startJob(@RequestBody @Validated JobCreationDTO jobCreationDTO) {
//        log.info("Received request to start job: {}", jobCreationDTO);
//        Job createdJob = jobService.createAndStartJob(jobCreationDTO);
//        return ResponseEntity.status(HttpStatus.CREATED).body(JobDTO.toDTO(createdJob));
//    }

    @PostMapping("/availability/create")
    public ResponseEntity<JobDTO> createAndStartAvailabilityJob(@RequestBody @Validated CreateAvailabilityJobDTO createAvailabilityJobDTO) {
        log.info("Received request to start job: {}", createAvailabilityJobDTO);
        Job createdJob = availabilityJobService.createAndStartAvailabilityJob(createAvailabilityJobDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobDTO.toDTO(createdJob));
    }

    @PostMapping("/search/create")
    public ResponseEntity<JobDTO> createAndStartSearchJob(@RequestBody CreateSearchJobDTO createSearchJobDTO) {
        log.info("Received request to create SEARCH job with params: {}", createSearchJobDTO);

        Job createdJob = searchJobService.createAndStartSearchJob(createSearchJobDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobDTO.toDTO(createdJob));
    }

    @PostMapping("/details/create")
    public ResponseEntity<JobDTO> createAndStartVehicleDetailsJob(@RequestBody CreateVehicleDetailsJobDTO createVehicleDetailsJobDTO) {
        log.info("Received request to create VEHICLE DETAILS job with params: {}", createVehicleDetailsJobDTO);

        Job createdJob = vehicleDetailsJobService.createAndStartVehicleDetailsJob(createVehicleDetailsJobDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobDTO.toDTO(createdJob));
    }


    @GetMapping("/{jobId}")
    public ResponseEntity<JobDTO> getJob(@PathVariable Long jobId) {
        Job job = jobService.getJob(jobId);
        return ResponseEntity.ok(JobDTO.toDTO(job));
    }

    @PostMapping("/{jobId}/incrementTotalItems")
    public ResponseEntity<JobDTO> incrementTotalItems(@PathVariable Long jobId, @RequestParam(defaultValue = "3") Integer increment) {
        Job job = jobService.incrementTotalItems(jobId, increment);
        return ResponseEntity.ok(JobDTO.toDTO(job));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Job.JobStatus> getJobStatus(@PathVariable Long jobId) {
        log.info("Received request to get status for job id: {}", jobId);
        Job.JobStatus status = null;
        try {
            status = jobService.getJobStatus(jobId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{jobId}/start")
    public ResponseEntity<JobDTO> startJob(@PathVariable Long jobId) {
        Job startedJob = jobService.startJob(jobId);
        return ResponseEntity.ok(JobDTO.toDTO(startedJob));
    }

    @PostMapping("/{jobId}/stop")
    public ResponseEntity<JobDTO> stopJob(@PathVariable Long jobId) {
        Job stoppedJob = jobService.stopJob(jobId);
        return ResponseEntity.ok(JobDTO.toDTO(stoppedJob));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }

}