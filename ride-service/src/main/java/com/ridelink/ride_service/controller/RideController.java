package com.ridelink.ride_service.controller;

import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.RideCancellation;
import com.ridelink.ride_service.model.Status;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @GetMapping("/{id}/status")
    @Operation(summary = "Get the current status of a ride")
    public ResponseEntity<RideDto.RideStatusResponse> getRideStatus(
            @PathVariable Integer id) {
        Ride ride = rideService.getRide(id);
        return ResponseEntity.ok(new RideDto.RideStatusResponse(
                ride.getId(),
                ride.getStatus(),
                latestActivity(ride),
                ride.getAssignedAt(),
                ride.getDriverId(),
                ride.getCancelledAt()));
    }

    private LocalDateTime latestActivity(Ride ride) {
        return Stream.of(ride.getCompletedTime(), ride.getCancelledAt(), ride.getStartTime(),
                        ride.getAssignedAt(), ride.getRequestedTime())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    @GetMapping("/passenger/me")
    @Operation(summary = "List rides for the authenticated passenger")
    public ResponseEntity<List<RideDto.RideRequestResponse>> getPassengerRides(
            @RequestHeader(USER_HEADER) @Positive Long passengerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Status filter = parseStatus(status);
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedTime"));
            Page<Ride> rides = rideService.getRidesForPassenger(passengerId, filter, pageable);
            return ResponseEntity.ok(rides.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        }
        List<Ride> rides = rideService.getRidesForPassenger(passengerId, filter);
        return ResponseEntity.ok(rides.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/driver/me")
    @Operation(summary = "List rides for the authenticated driver")
    public ResponseEntity<List<RideDto.RideRequestResponse>> getDriverRides(
            @RequestHeader(USER_HEADER) @Positive Long driverId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Status filter = parseStatus(status);
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
            Page<Ride> rides = rideService.getRidesForDriver(driverId, filter, pageable);
            return ResponseEntity.ok(rides.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        }
        List<Ride> rides = rideService.getRidesForDriver(driverId, filter);
        return ResponseEntity.ok(rides.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Start a ride (transition ASSIGNED -> ONGOING)")
    public ResponseEntity<RideDto.RideRequestResponse> startRide(
            @PathVariable Integer id,
            @RequestHeader(USER_HEADER) @Positive Long driverId) {
        Ride started = rideService.startRide(id, driverId);
        return ResponseEntity.ok(toResponse(started));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Complete a ride (transition ONGOING -> COMPLETED)")
    public ResponseEntity<RideDto.RideRequestResponse> completeRide(
            @PathVariable Integer id,
            @RequestHeader(USER_HEADER) @Positive Long callerId) {
        Ride completed = rideService.completeRide(id, callerId);
        return ResponseEntity.ok(toResponse(completed));
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

    private Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid status: " + status);
        }
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
                ride.getCancelledBy());
    }
}
