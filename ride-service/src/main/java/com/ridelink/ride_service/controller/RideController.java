package com.ridelink.ride_service.controller;

import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/ride")
public class RideController {

    // Placeholder until ride-service has its own auth.
    private static final String PASSENGER_HEADER = "X-User-Id";

    @Autowired
    private RideService rideService;

    @PostMapping("/request")
    public ResponseEntity<RideDto.RideRequestResponse> createRideRequest(
            @RequestHeader(PASSENGER_HEADER) @Positive Long passengerId,
            @Valid @RequestBody RideDto.RideRequest request) {
        Ride created = rideService.createRideRequest(passengerId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/ride/" + created.getId()))
                .body(toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideDto.RideRequestResponse> getRide(
            @PathVariable Integer id,
            @RequestHeader(PASSENGER_HEADER) @Positive Long passengerId) {
        Ride ride = rideService.getRideForPassenger(id, passengerId);
        return ResponseEntity.ok(toResponse(ride));
    }

    private RideDto.RideRequestResponse toResponse(Ride ride) {
        return new RideDto.RideRequestResponse(
                ride.getId(),
                ride.getUserId(),
                ride.getDriverId(),
                ride.getStatus(),
                ride.getPickupLocation(),
                ride.getDropOffLocation(),
                ride.getTripDistance(),
                ride.getRequestedTime(),
                ride.getStartTime(),
                ride.getCompletedTime()
        );
    }
}
