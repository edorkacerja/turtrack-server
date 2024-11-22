package com.turtrack.server.service.turtrack;

import com.turtrack.server.model.turtrack.Vehicle;
import com.turtrack.server.repository.turtrack.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public List<Vehicle> getAllVehicles() {
        log.info("Fetching all vehicles");
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getVehiclesWithLimit(int limit) {
        return vehicleRepository.findAll(PageRequest.of(0, limit)).getContent();
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        log.info("Fetching vehicle with id: {}", id);
        return vehicleRepository.findById(id);
    }

    public Vehicle saveVehicle(Vehicle vehicle) {
        log.info("Saving vehicle: {}", vehicle);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> saveAllVehicles(List<Vehicle> vehicles) {
        log.info("Saving a list of {} vehicles", vehicles.size());
        return vehicleRepository.saveAll(vehicles);
    }

    public Vehicle updateVehicle(Long id, Vehicle updatedVehicle) {
        log.info("Updating vehicle with id: {}", id);
        if (vehicleRepository.existsById(id)) {
            updatedVehicle.setId(id);
            return vehicleRepository.save(updatedVehicle);
        }
        throw new RuntimeException("Vehicle not found with id: " + id);
    }

    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle with id: {}", id);
        vehicleRepository.deleteById(id);
    }

    public List<Vehicle> getVehiclesWithLimitAndOffset(int limit, int offset) {
        if (limit < 0 || offset < 0) {
            throw new IllegalArgumentException("Limit and offset must be non-negative");
        }

        if (limit == 0) {
            // If limit is 0, return all vehicles
            return vehicleRepository.findAll();
        } else {
            // Calculate the page number and use the provided limit as page size
            int pageNumber = offset / limit;
            Pageable pageable = PageRequest.of(pageNumber, limit);
            return vehicleRepository.findAll(pageable).getContent();
        }
    }
}