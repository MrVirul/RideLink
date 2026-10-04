package com.ridelink.fare_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReceiptResponse", description = "Receipt issued for a recorded payment")
public record ReceiptResponse(
        @Schema(description = "Human readable receipt number", example = "RCPT-2026-000007")
        String receiptNumber,
        @Schema(description = "Id of the payment this receipt is for", example = "7")
        Long paymentId,
        @Schema(description = "Id of the ride that was paid for", example = "42")
        Long rideId,
        @Schema(description = "Amount paid, in LKR", example = "1010.00")
        BigDecimal amount,
        @Schema(description = "How the ride was paid for", example = "CARD")
        String paymentMethod,
        @Schema(description = "Status of the payment at the time the receipt was issued", example = "PAID")
        String paymentStatus,
        @Schema(description = "Reference issued for the transaction", example = "TXN-8F3A21C4")
        String transactionReference,
        @Schema(description = "When the payment was recorded", example = "2026-09-28T10:44:07")
        LocalDateTime paidAt,
        @Schema(description = "Human readable summary shown on the receipt", example = "Payment of LKR 1010.00 received for ride 42")
        String message) {
}