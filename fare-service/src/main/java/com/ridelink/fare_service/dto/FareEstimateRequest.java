package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record FareEstimateRequest(
        @NotNull(message = "Ride ID is required") Long rideId,
        @NotNull(message = "Distance must be provided")
        @DecimalMin(value = "0", inclusive = false, message = "Distance must be greater than 0")
        BigDecimal distanceKm) {
}
