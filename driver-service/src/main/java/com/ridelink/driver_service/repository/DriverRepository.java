package com.ridelink.driver_service.repository;

import com.ridelink.driver_service.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    // find all currently available drivers
    List<Driver> findByIsAvailable(boolean isAvailable);

    List<Driver> id(Integer id);

}
