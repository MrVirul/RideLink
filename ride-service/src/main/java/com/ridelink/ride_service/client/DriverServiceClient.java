package com.ridelink.ride_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "DRIVER-SERVICE")
public interface DriverServiceClient {

    @GetMapping("/api/v1/driver/eligible")
    List<AvailableDriver> getAvailableDrivers();
}
