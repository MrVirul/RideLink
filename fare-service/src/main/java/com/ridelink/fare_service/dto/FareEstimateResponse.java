package com.ridelink.fare_service.dto;

public class FareEstimateResponse {

        private Long rideId;
        private double distanceKm;
        private double estimatedFare;

        public FareEstimateResponse(Long rideId, double distanceKm, double estimatedFare) {
                this.rideId = rideId;
                this.distanceKm = distanceKm;
                this.estimatedFare = estimatedFare;
        }

        public Long getRideId() {
                return rideId;
        }

        public double getDistanceKm() {
                return distanceKm;
        }

        public double getEstimatedFare() {
                return estimatedFare;
        }
}