package com.ridelink.ride_service.Service;

import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.RideCancellation;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.repository.RideCancellationRepository;
import com.ridelink.ride_service.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class RideService {

    private static final List<Status> ACTIVE_STATUSES = List.of(Status.SEARCHING, Status.ONGOING);

    private static final List<Status> PASSENGER_CANCELLABLE_STATUSES = List.of(Status.SEARCHING, Status.ONGOING);

    private static final List<Status> DRIVER_CANCELLABLE_STATUSES = List.of(Status.SEARCHING);

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideCancellationRepository rideCancellationRepository;

    @Transactional
    public Ride createRideRequest(Long passengerId, RideDto.RideRequest request) {
        rejectIfRideAlreadyActive(passengerId);

        Ride ride = new Ride();
        ride.setUserId(passengerId);
        ride.setStatus(Status.SEARCHING);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropOffLocation(request.getDropOffLocation());
        ride.setTripDistance(request.getTripDistance());
        ride.setRequestedTime(LocalDateTime.now());
        return rideRepository.save(ride);
    }

    @Transactional(readOnly = true)
    public Ride getRideForPassenger(Integer rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ride " + rideId + " not found"));

        if (!ride.getUserId().equals(passengerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Ride " + rideId + " belongs to another passenger");
        }
        return ride;
    }

    @Transactional
    public Ride cancelRide(Integer rideId, Long callerId, RideDto.CancelRideRequest request) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ride " + rideId + " not found"));

        boolean isPassenger = ride.getUserId().equals(callerId);
        boolean isAssignedDriver = callerId.equals(ride.getDriverId());
        if (!isPassenger && !isAssignedDriver) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Caller " + callerId + " is not part of ride " + rideId);
        }

        List<Status> cancellable = isPassenger ? PASSENGER_CANCELLABLE_STATUSES : DRIVER_CANCELLABLE_STATUSES;
        if (!cancellable.contains(ride.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ride " + rideId + " cannot be cancelled by its "
                            + (isPassenger ? "passenger" : "driver") + " from state " + ride.getStatus());
        }

        Status previousStatus = ride.getStatus();
        LocalDateTime cancelledAt = LocalDateTime.now();

        ride.setStatus(Status.CANCELLED);
        ride.setCancelledAt(cancelledAt);
        ride.setCancelledBy(callerId);
        Ride saved = rideRepository.save(ride);

        RideCancellation cancellation = new RideCancellation();
        cancellation.setRideId(saved.getId());
        cancellation.setPreviousStatus(previousStatus);
        cancellation.setCancelledBy(callerId);
        cancellation.setCancelledAt(cancelledAt);
        cancellation.setReason(request == null ? null : request.getReason());
        rideCancellationRepository.save(cancellation);

        return saved;
    }

    @Transactional(readOnly = true)
    public RideCancellation getCancellation(Integer rideId, Long callerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ride " + rideId + " not found"));

        if (!ride.getUserId().equals(callerId) && !callerId.equals(ride.getDriverId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Caller " + callerId + " is not part of ride " + rideId);
        }

        return rideCancellationRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ride " + rideId + " was never cancelled"));
    }

    private void rejectIfRideAlreadyActive(Long passengerId) {
        rideRepository.findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(passengerId, ACTIVE_STATUSES)
                .ifPresent(active -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Passenger " + passengerId + " already has ride " + active.getId()
                                    + " in state " + active.getStatus());
                });
    }
}
