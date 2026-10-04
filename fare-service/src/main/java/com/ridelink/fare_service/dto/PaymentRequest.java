package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "PaymentRequest", description = "Payment for a completed ride. Processing is simulated, no card details are taken.")
public record PaymentRequest(
        @Schema(description = "Id of the ride being paid for", example = "42")
        @NotNull(message = "Ride ID is required") Long rideId,
        @Schema(description = "Amount being paid, in LKR, must be greater than 0", example = "1010.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0", inclusive = false, message = "Amount must be greater than 0")
        BigDecimal amount,
        @Schema(description = "How the ride was paid for, one of CASH, CARD or WALLET", example = "CARD",
                allowableValues = {"CASH", "CARD", "WALLET"})
        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "(?i)CASH|CARD|WALLET", message = "Payment method must be CASH, CARD, or WALLET")
        String paymentMethod) {
}