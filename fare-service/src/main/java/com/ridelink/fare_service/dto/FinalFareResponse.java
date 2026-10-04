package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FinalFareResponse", description = "Final price for a completed ride, priced on the distance actually driven")
public record FinalFareResponse(
        @Schema(description = "Id of the completed ride", example = "42")
        Long rideId,
        @Schema(description = "Distance actually driven, in kilometres", example = "8.1")
        BigDecimal actualDistanceKm,
        @Schema(description = "Final fare charged, in LKR", example = "1010.00")
        BigDecimal finalFare) {
}