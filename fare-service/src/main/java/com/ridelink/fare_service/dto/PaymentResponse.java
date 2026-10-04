package com.ridelink.fare_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaymentResponse", description = "A recorded payment for a ride")
public record PaymentResponse(
        @Schema(description = "Id of the payment", example = "7")
        Long paymentId,
        @Schema(description = "Id of the ride that was paid for", example = "42")
        Long rideId,
        @Schema(description = "Amount paid, in LKR", example = "1010.00")
        BigDecimal amount,
        @Schema(description = "How the ride was paid for", example = "CARD")
        String paymentMethod,
        @Schema(description = "Status of the payment", example = "PAID")
        String status,
        @Schema(description = "Reference issued for the transaction", example = "TXN-8F3A21C4")
        String transactionReference,
        @Schema(description = "When the payment was recorded", example = "2026-09-28T10:44:07")
        LocalDateTime paidAt) {
}