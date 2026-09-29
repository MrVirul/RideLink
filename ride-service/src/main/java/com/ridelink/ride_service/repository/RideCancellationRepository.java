package com.ridelink.ride_service.repository;

import com.ridelink.ride_service.model.RideCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RideCancellationRepository extends JpaRepository<RideCancellation, Integer> {

    Optional<RideCancellation> findByRideId(Integer rideId);

}
