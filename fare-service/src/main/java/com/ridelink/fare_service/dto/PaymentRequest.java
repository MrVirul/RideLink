package com.ridelink.fare_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotNull(message = "Ride ID is required") Long rideId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0", inclusive = false, message = "Amount must be greater than 0")
        BigDecimal amount,
        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "(?i)CASH|CARD|WALLET", message = "Payment method must be CASH, CARD, or WALLET")
        String paymentMethod) {
}