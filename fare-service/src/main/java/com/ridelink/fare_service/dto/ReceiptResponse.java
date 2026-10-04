package com.ridelink.fare_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
        String receiptNumber,
        Long paymentId,
        Long rideId,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        String transactionReference,
        LocalDateTime paidAt,
        String message) {
}