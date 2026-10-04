package com.ridelink.ride_service.repository;

import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Integer> {

    Optional<Ride> findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(Long userId, Collection<Status> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") Integer id);

    List<Ride> findByUserIdOrderByRequestedTimeDesc(Long userId);

    List<Ride> findByUserIdAndStatusOrderByRequestedTimeDesc(Long userId, Status status);

    Page<Ride> findByUserIdOrderByRequestedTimeDesc(Long userId, Pageable pageable);

    Page<Ride> findByUserIdAndStatusOrderByRequestedTimeDesc(Long userId, Status status, Pageable pageable);

    List<Ride> findByDriverIdOrderByAssignedAtDesc(Long driverId);

    List<Ride> findByDriverIdAndStatusOrderByAssignedAtDesc(Long driverId, Status status);

    Page<Ride> findByDriverIdOrderByAssignedAtDesc(Long driverId, Pageable pageable);

    Page<Ride> findByDriverIdAndStatusOrderByAssignedAtDesc(Long driverId, Status status, Pageable pageable);

}
