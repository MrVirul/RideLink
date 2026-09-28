package com.ridelink.ride_service.Service;

import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
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

    @Autowired
    private RideRepository rideRepository;

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
