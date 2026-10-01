package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal durationFare,
        BigDecimal totalFare,
        String currency) {
}