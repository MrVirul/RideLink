package com.ridelink.fare_service.service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import org.springframework.stereotype.Service;

@Service
public class FareService {

    private static final double BASE_FARE = 200.0;
    private static final double RATE_PER_KM = 100.0;

    public FareEstimateResponse calculateEstimate(FareEstimateRequest request) {
        double estimatedFare = BASE_FARE + (request.getDistanceKm() * RATE_PER_KM);

        return new FareEstimateResponse(
                request.getRideId(),
                request.getDistanceKm(),
                estimatedFare);
    }
}