package com.ridelink.fare_service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.dto.FinalFareRequest;
import com.ridelink.fare_service.dto.FinalFareResponse;
import com.ridelink.fare_service.service.FareService;

class FareServiceTests {

    private final FareService fareService = new FareService();

    @Test
    void estimatesFareForFiveKilometers() {
        FareEstimateResponse response = fareService.calculateEstimate(
                new FareEstimateRequest(101L, new BigDecimal("5")));

        assertEquals(new BigDecimal("700.00"), response.estimatedFare());
    }

    @Test
    void estimatesFareForTenKilometers() {
        FareEstimateResponse response = fareService.calculateEstimate(
                new FareEstimateRequest(102L, new BigDecimal("10")));

        assertEquals(new BigDecimal("1200.00"), response.estimatedFare());
    }

    @Test
    void calculatesFinalFareUsingActualDistance() {
        FinalFareResponse response = fareService.calculateFinalFare(
                new FinalFareRequest(101L, new BigDecimal("6")));

        assertEquals(new BigDecimal("800.00"), response.finalFare());
    }
}