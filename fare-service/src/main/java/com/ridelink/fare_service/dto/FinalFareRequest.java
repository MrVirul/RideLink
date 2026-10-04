package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record FinalFareRequest(
        @NotNull(message = "Ride ID is required") Long rideId,
        @NotNull(message = "Actual distance must be provided")
        @DecimalMin(value = "0", inclusive = false, message = "Actual distance must be greater than 0")
        BigDecimal actualDistanceKm) {
}