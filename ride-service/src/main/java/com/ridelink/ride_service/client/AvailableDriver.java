package com.ridelink.ride_service.client;

public record AvailableDriver(
        Integer id,
        Long userId,
        String vehicleNumberPlate,
        double latitude,
        double longitude,
        String serviceArea,
        boolean available
) {
}
