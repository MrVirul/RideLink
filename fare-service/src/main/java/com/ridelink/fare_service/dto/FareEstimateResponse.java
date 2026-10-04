package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FareEstimateResponse", description = "Estimated price for a ride, priced on the quoted distance")
public record FareEstimateResponse(
        @Schema(description = "Id of the ride that was estimated", example = "42")
        Long rideId,
        @Schema(description = "Distance the estimate was priced on, in kilometres", example = "7.4")
        BigDecimal distanceKm,
        @Schema(description = "Estimated fare, in LKR", example = "940.00")
        BigDecimal estimatedFare) {
}