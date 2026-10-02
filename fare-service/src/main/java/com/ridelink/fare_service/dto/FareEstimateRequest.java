package com.ridelink.fare_service.dto;

public class FareEstimateRequest {

    private Long rideId;
    private double distanceKm;
    private long durationMinutes;  // ADD THIS FIELD

    public FareEstimateRequest() {
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    // ADD THESE METHODS
    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
