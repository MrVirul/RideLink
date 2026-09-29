package com.ridelink.ride_service.Service;

import com.ridelink.ride_service.client.AvailableDriver;
import com.ridelink.ride_service.client.DriverServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DriverAvailabilityService {

    @Autowired
    private DriverServiceClient driverServiceClient;

    public List<AvailableDriver> findAvailableDrivers() {
        List<AvailableDriver> drivers = driverServiceClient.getAvailableDrivers();
        if (drivers == null || drivers.isEmpty()) {
            log.warn("driver-service reported no available drivers");
            return List.of();
        }
        return drivers;
    }
}
