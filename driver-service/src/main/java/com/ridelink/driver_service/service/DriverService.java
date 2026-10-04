package com.ridelink.driver_service.service;

import com.ridelink.driver_service.model.AvailabilityHistory;
import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.AvailabilityHistoryRepository;
import com.ridelink.driver_service.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private AvailabilityHistoryRepository availabilityHistoryRepository;

    public Driver registerDriver(Driver driver) {
        driver.setAvailable(true);
        driver.setLastOnlineAt(LocalDateTime.now());
        return driverRepository.save(driver);
    }

    // Retained for backward compatibility
    public Driver reigsterDriver(Driver driver) {
        return registerDriver(driver);
    }

    public Driver updateLocation(Integer id, double latitude, double longitude, Double accuracy) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        validateCoordinatesWithinServiceArea(driver.getServiceArea(), latitude, longitude);

        driver.setLatitude(latitude);
        driver.setLongitude(longitude);
        driver.setAccuracy(accuracy != null ? accuracy : 0.0);
        driver.setLastOnlineAt(LocalDateTime.now());

        return driverRepository.save(driver);
    }

    private void validateCoordinatesWithinServiceArea(String serviceArea, double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Coordinates out of Valid Range");
        }
    }

    public Driver updateAvailability(Integer id, boolean isAvailable) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setAvailable(isAvailable);
        if (isAvailable) {
            driver.setLastOnlineAt(LocalDateTime.now());
        }
        Driver savedDriver = driverRepository.save(driver);

        AvailabilityHistory history = new AvailabilityHistory(
                driver.getId(),
                isAvailable,
                LocalDateTime.now()
        );
        availabilityHistoryRepository.save(history);

        return savedDriver;
    }

    public Driver toggleAvailability(Integer id, Boolean explicitAvailability) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        boolean newStatus = (explicitAvailability != null) ? explicitAvailability : !driver.isAvailable();
        return updateAvailability(id, newStatus);
    }

    public List<AvailabilityHistory> getAvailabilityHistory(Integer driverId) {
        return availabilityHistoryRepository.findByDriverIdOrderByChangedAtDesc(driverId);
    }

    public List<Driver> getEligibleAvailableDrivers() {
        return driverRepository.findByIsAvailable(true);
    }
}