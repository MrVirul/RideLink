package com.ridelink.ride_service.controller;

import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.RideCancellation;
import io.swagger.v3.oas.annotations.Operation;
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
    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private RideService rideService;

    @PostMapping("/request")
    @Operation(summary = "Request a ride")
    public ResponseEntity<RideDto.RideRequestResponse> createRideRequest(
            @RequestHeader(USER_HEADER) @Positive Long passengerId,
            @Valid @RequestBody RideDto.RideRequest request) {
        Ride created = rideService.createRideRequest(passengerId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/ride/" + created.getId()))
                .body(toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the caller's rides")
    public ResponseEntity<RideDto.RideRequestResponse> getRide(
            @PathVariable Integer id,
            @RequestHeader(USER_HEADER) @Positive Long passengerId) {
        Ride ride = rideService.getRideForPassenger(id, passengerId);
        return ResponseEntity.ok(toResponse(ride));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign an eligible driver to a searching ride")
    public ResponseEntity<RideDto.RideRequestResponse> assignDriver(
            @PathVariable Integer id) {
        Ride assigned = rideService.assignDriver(id);
        return ResponseEntity.ok(toResponse(assigned));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a ride")
    public ResponseEntity<RideDto.RideRequestResponse> cancelRide(
            @PathVariable Integer id,
            @RequestHeader(USER_HEADER) @Positive Long callerId,
            @Valid @RequestBody(required = false) RideDto.CancelRideRequest request) {
        Ride cancelled = rideService.cancelRide(id, callerId, request);
        return ResponseEntity.ok(toResponse(cancelled));
    }

    @GetMapping("/{id}/cancellation")
    @Operation(summary = "Get the cancellation record for a cancelled ride")
    public ResponseEntity<RideDto.CancellationResponse> getCancellation(
            @PathVariable Integer id,
            @RequestHeader(USER_HEADER) @Positive Long callerId) {
        RideCancellation cancellation = rideService.getCancellation(id, callerId);
        return ResponseEntity.ok(new RideDto.CancellationResponse(
                cancellation.getRideId(),
                cancellation.getPreviousStatus(),
                cancellation.getCancelledBy(),
                cancellation.getCancelledAt(),
                cancellation.getReason()
        ));
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
                ride.getAssignedAt(),
                ride.getStartTime(),
                ride.getCompletedTime(),
                ride.getCancelledAt(),
                ride.getCancelledBy()
        );
    }
}
