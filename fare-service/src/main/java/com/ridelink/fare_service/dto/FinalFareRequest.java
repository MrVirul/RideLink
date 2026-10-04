package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(name = "FinalFareRequest", description = "Distance actually covered by a completed ride, used to charge the final fare")
public record FinalFareRequest(
        @Schema(description = "Id of the completed ride", example = "42")
        @NotNull(message = "Ride ID is required") Long rideId,
        @Schema(description = "Distance actually driven in kilometres, must be greater than 0", example = "8.1")
        @NotNull(message = "Actual distance must be provided")
        @DecimalMin(value = "0", inclusive = false, message = "Actual distance must be greater than 0")
        BigDecimal actualDistanceKm) {
}