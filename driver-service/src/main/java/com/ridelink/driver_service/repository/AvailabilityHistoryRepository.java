package com.ridelink.driver_service.repository;

import com.ridelink.driver_service.model.AvailabilityHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilityHistoryRepository extends JpaRepository<AvailabilityHistory, Integer> {

    List<AvailabilityHistory> findByDriverIdOrderByChangedAtDesc(Integer driverId);
}
