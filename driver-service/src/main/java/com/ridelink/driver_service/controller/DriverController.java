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
@RequestMapping({"/api/v1/driver", "/api/v1/drivers"})
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping
    public ResponseEntity<Driver> registerDriver(@Valid @RequestBody Driver driver) {
        Driver savedDriver = driverService.registerDriver(driver);
        return new ResponseEntity<>(savedDriver, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Driver> updateLocation(
            @PathVariable Integer id,
            @Valid @RequestBody DriverDto.LocationRequest request) {

        Driver updated = driverService.updateLocation(id, request.getLatitude(), request.getLongitude(),
                request.getAccuracy());
        return ResponseEntity.ok(updated);
    }

    @RequestMapping(
            value = "/{driverId}/availability",
            method = {RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<Driver> toggleAvailability(
            @PathVariable("driverId") Integer driverId,
            @RequestBody(required = false) DriverDto.AvailabilityRequest request) {
        Boolean explicitAvailable = (request != null) ? request.getAvailable() : null;
        Driver updated = driverService.toggleAvailability(driverId, explicitAvailable);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/eligible")
    public ResponseEntity<List<Driver>> getEligibleDrivers() {
        List<Driver> drivers = driverService.getEligibleAvailableDrivers();
        return ResponseEntity.ok(drivers);
    }
}
