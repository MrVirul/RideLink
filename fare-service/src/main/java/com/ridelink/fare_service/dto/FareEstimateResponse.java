package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(Long rideId, BigDecimal distanceKm, BigDecimal estimatedFare) {
}