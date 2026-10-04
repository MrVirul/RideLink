package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(name = "FareEstimateRequest", description = "Quoted distance for a ride, used to price it before the trip starts")
public record FareEstimateRequest(
        @Schema(description = "Id of the ride being estimated", example = "42")
        @NotNull(message = "Ride ID is required") Long rideId,
        @Schema(description = "Trip distance in kilometres, must be greater than 0", example = "7.4")
        @NotNull(message = "Distance must be provided")
        @DecimalMin(value = "0", inclusive = false, message = "Distance must be greater than 0")
        BigDecimal distanceKm) {
}