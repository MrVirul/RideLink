package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

public record FinalFareResponse(Long rideId, BigDecimal actualDistanceKm, BigDecimal finalFare) {
}