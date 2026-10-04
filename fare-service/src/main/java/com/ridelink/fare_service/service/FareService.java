package com.ridelink.fare_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.dto.FinalFareRequest;
import com.ridelink.fare_service.dto.FinalFareResponse;

@Service
public class FareService {

    private static final BigDecimal BASE_FARE = new BigDecimal("200.00");
    private static final BigDecimal RATE_PER_KM = new BigDecimal("100.00");

    public FareEstimateResponse calculateEstimate(FareEstimateRequest request) {
        BigDecimal estimatedFare = calculateFare(request.distanceKm());

        return new FareEstimateResponse(
                request.rideId(),
                request.distanceKm(),
                estimatedFare);
    }

    public FinalFareResponse calculateFinalFare(FinalFareRequest request) {
        return new FinalFareResponse(
                request.rideId(),
                request.actualDistanceKm(),
                calculateFare(request.actualDistanceKm()));
    }

    private BigDecimal calculateFare(BigDecimal distanceKm) {
        return BASE_FARE.add(distanceKm.multiply(RATE_PER_KM)).setScale(2, RoundingMode.HALF_UP);
    }
}
