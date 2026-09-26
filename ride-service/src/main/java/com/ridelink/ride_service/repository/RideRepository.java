package com.ridelink.ride_service.repository;

import com.ridelink.ride_service.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, Integer> {

}
