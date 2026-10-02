package com.ridelink.fare_service.service;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareService {

    private static final BigDecimal BASE_FARE = new BigDecimal("100.00");
    private static final BigDecimal FARE_PER_KILOMETER = new BigDecimal("50.00");
    private static final BigDecimal FARE_PER_MINUTE = new BigDecimal("10.00");
    private static final String CURRENCY = "LKR";

    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        BigDecimal distanceFare = BigDecimal.valueOf(request.getDistanceKm())
                .multiply(FARE_PER_KILOMETER)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationFare = FARE_PER_MINUTE
                .multiply(BigDecimal.valueOf(request.getDurationMinutes()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFare = BASE_FARE
                .add(distanceFare)
                .add(durationFare)
                .setScale(2, RoundingMode.HALF_UP);

        return new FareEstimateResponse(
                BASE_FARE,
                distanceFare,
                durationFare,
                totalFare,
                CURRENCY);
    }
}
