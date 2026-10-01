package com.ridelink.fare_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FareEstimateRequest(
        @NotNull @DecimalMin("0.0") BigDecimal distanceKm,
        @NotNull @Min(0) Integer durationMinutes) {
}