package com.turtrack.server.service.turtrack;

import com.turtrack.server.model.manager.Job;
import com.turtrack.server.model.turtrack.DailyRateAndAvailability;
import com.turtrack.server.model.turtrack.Vehicle;
import com.turtrack.server.repository.manager.JobRepository;
import com.turtrack.server.repository.turtrack.DailyRateAndAvailabilityRepository;
import com.turtrack.server.repository.turtrack.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyRateAndAvailabilityService {

    private final JobRepository jobRepository;
    private final DailyRateAndAvailabilityRepository dailyRateRepository;
    private final VehicleRepository vehicleRepository;


    @Transactional
    public void processFailedVehicle(Map<String, Object> message) {
        Long jobId = extractJobId(message);
        Job job = getJobById(jobId);
        incrementFailedItems(job, 1);
        jobRepository.save(job);
        log.info("Updated job for failed vehicle: jobId={}, failedItems={}, percentCompleted={}%, status={}",
                jobId, job.getFailedItems(), job.getPercentCompleted(), job.getStatus());
    }

    @Transactional
    public void consumeScrapedDailyRates(Map<String, Object> message) {
        Long externalVehicleId = extractVehicleId(message);
        Vehicle vehicle = vehicleRepository.findByExternalId(externalVehicleId).orElseThrow();
        Long vehicleId = vehicle.getId();

        Long jobId = extractJobId(message);
        List<DailyRateAndAvailability> dailyRates = extractDailyRates(message, vehicleId);


        dailyRateRepository.saveAll(dailyRates);

        Job job = getJobById(jobId);
        incrementCompletedItems(job, 1);
        jobRepository.save(job);
        log.info("Updated job progress: jobId={}, completedItems={}, percentCompleted={}%",
                jobId, job.getCompletedItems(), job.getPercentCompleted());
    }

    private Job getJobById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
    }

    private void incrementFailedItems(Job job, int increment) {
        int failedItems = Optional.ofNullable(job.getFailedItems()).orElse(0) + increment;
        job.setFailedItems(failedItems);
        updateJobCompletionStatus(job);
    }

    private void incrementCompletedItems(Job job, int increment) {
        int completedItems = Optional.ofNullable(job.getCompletedItems()).orElse(0) + increment;
        job.setCompletedItems(completedItems);
        updateJobCompletionStatus(job);
    }

    private void updateJobCompletionStatus(Job job) {
        Integer totalItems = job.getTotalItems();
        if (totalItems != null && totalItems > 0) {
            int processedItems = Optional.ofNullable(job.getCompletedItems()).orElse(0)
                    + Optional.ofNullable(job.getFailedItems()).orElse(0);
            double percentCompleted = (double) processedItems / totalItems * 100;
            job.setPercentCompleted(percentCompleted);

            if (processedItems >= totalItems) {
                job.setStatus(Job.JobStatus.FINISHED);
                job.setFinishedAt(LocalDateTime.now());
            }
        } else {
            log.warn("Job {} has no total items set. Unable to calculate percent completed.", job.getId());
        }
    }

    private Long extractVehicleId(Map<String, Object> message) {
        Object vehicleIdObj = message.get("vehicleId");
        if (vehicleIdObj == null) {
            throw new IllegalArgumentException("vehicleId is missing in message");
        }
        return parseLong(vehicleIdObj, "vehicleId");
    }

    private Long extractJobId(Map<String, Object> message) {
        Object jobIdObj = message.get("jobId");
        if (jobIdObj == null) {
            throw new IllegalArgumentException("jobId is missing in message");
        }
        return parseLong(jobIdObj, "jobId");
    }

    private Long parseLong(Object value, String fieldName) {
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for " + fieldName + ": " + value, e);
        }
        throw new IllegalArgumentException("Cannot parse " + fieldName + " from: " + value);
    }

    private List<DailyRateAndAvailability> extractDailyRates(Map<String, Object> message, Long vehicleId) {

        Object scraped = message.get("scraped");
        if (!(scraped instanceof Map)) {
            throw new IllegalArgumentException("dailyPricingResponses is missing or not a map");
        }

        Map<?,?> scraped1 = (Map<?,?>) scraped;
        Object responsesObj = scraped1.get("dailyPricingResponses");

        if (!(responsesObj instanceof List)) {
            throw new IllegalArgumentException("dailyPricingResponses is missing or not a list");
        }

        List<DailyRateAndAvailability> dailyRates = new ArrayList<>();
        List<?> dailyPricingResponses = (List<?>) responsesObj;

        for (Object obj : dailyPricingResponses) {
            if (obj instanceof Map) {
                Map<String, Object> dailyPricing = (Map<String, Object>) obj;
                try {
                    DailyRateAndAvailability dailyRate = createDailyRate(dailyPricing, vehicleId);
                    dailyRates.add(dailyRate);
                } catch (Exception e) {
                    log.warn("Failed to create DailyRateAndAvailability from dailyPricing: {}, error: {}",
                            dailyPricing, e.getMessage());
                }
            } else {
                log.warn("Invalid dailyPricingResponse entry: {}", obj);
            }
        }
        return dailyRates;
    }

    private DailyRateAndAvailability createDailyRate(Map<String, Object> dailyPricing, Long vehicleId) {
        try {
            String dateStr = (String) dailyPricing.get("date");
            LocalDate date = LocalDate.parse(dateStr);

            DailyRateAndAvailability dailyRate = new DailyRateAndAvailability();
            dailyRate.setId(new DailyRateAndAvailability.DailyRateAndAvailabilityId(vehicleId, date));
            dailyRate.setLocalizedDayOfWeek((String) dailyPricing.get("localizedDayOfWeek"));
            dailyRate.setPrice(convertToDouble(dailyPricing.get("price")));
            dailyRate.setWholeDayUnavailable((Boolean) dailyPricing.get("wholeDayUnavailable"));
            dailyRate.setCustomSetPrice((Boolean) dailyPricing.get("custom"));

            Map<String, Object> priceWithCurrency = (Map<String, Object>) dailyPricing.get("priceWithCurrency");
            if (priceWithCurrency != null) {
                dailyRate.setCurrencyCode((String) priceWithCurrency.get("currencyCode"));
            }

            return dailyRate;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing dailyPricing: " + dailyPricing, e);
        }
    }

    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse double from value: " + value, e);
        }
        throw new IllegalArgumentException("Cannot convert value to Double: " + value);
    }

}
