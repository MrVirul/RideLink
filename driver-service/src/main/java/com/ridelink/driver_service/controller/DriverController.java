package com.ridelink.driver_service.controller;

import com.ridelink.driver_service.dto.DriverDto;
import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.DriverRepository;
import com.ridelink.driver_service.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;
    @Autowired
    private DriverRepository driverRepository;

    @PostMapping
    public ResponseEntity<Driver> reigsterDriver(@Valid @RequestBody Driver driver) {
        Driver saveDriver = driverService.reigsterDriver(driver);
        return new ResponseEntity<>(saveDriver, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Driver> updateLocation(
            @PathVariable Integer id,
            @RequestBody DriverDto.LocationRequest request) {
        Driver updated = driverService.updateLocation(id, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/availability")
        public ResponseEntity<Driver> updateAvailability(
                @PathVariable Integer id,
                @RequestBody DriverDto.AvailabilityRequest request) {
            Driver updated = driverService.updateAvailability(id, request.getAvailable());
            return ResponseEntity.ok(updated);
        }

    @GetMapping("/eligible")
    public ResponseEntity<List<Driver>> getEligibleDrivers(
            @RequestParam(required = false) String serviceArea) {
        List<Driver> drivers = driverService.getEligibleAvailableDrivers(serviceArea);
        return ResponseEntity.ok(drivers);
    }
}
