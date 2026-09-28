package com.ridelink.ride_service.repository;

import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Integer> {

    Optional<Ride> findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(Long userId, Collection<Status> statuses);

}
