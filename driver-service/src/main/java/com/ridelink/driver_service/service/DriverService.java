package com.ridelink.driver_service.service;

import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    public Driver reigsterDriver(Driver driver) {
        driver.setAvailable(true);
        return driverRepository.save(driver);
    }

    public Driver updateLocation(Integer id, double latitude, double longtitude, Double accuracy) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        validateCoordinatesWithinServiceArea(driver.getServiceArea(), latitude, longtitude);

        driver.setLatitude(latitude);
        driver.setLongitude(longtitude);
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
        return driverRepository.save(driver);
    }

    public List<Driver> getEligibleAvailableDrivers() {
        return driverRepository.findByIsAvailable(true);
    }
}