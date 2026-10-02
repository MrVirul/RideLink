package com.ridelink.fare_service.dto;

public class FareEstimateRequest {

	private Long rideId;
	private double distanceKm;

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
}