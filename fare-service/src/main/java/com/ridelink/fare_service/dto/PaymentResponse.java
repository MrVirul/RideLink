package com.ridelink.fare_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long rideId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String transactionReference,
        LocalDateTime paidAt) {
}