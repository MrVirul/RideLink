package com.ridelink.fare_service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridelink.fare_service.dto.PaymentRequest;
import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.entity.Payment;
import com.ridelink.fare_service.repository.PaymentRepository;
import com.ridelink.fare_service.service.PaymentService;

class PaymentServiceTests {

    @Test
    void createsAndPersistsSimulatedPayment() {
        PaymentRepository repository = mock(PaymentRepository.class);
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        PaymentService paymentService = new PaymentService(repository);

        PaymentResponse response = paymentService.createPayment(
                new PaymentRequest(101L, new BigDecimal("800.00"), "card"));

        assertEquals(1L, response.paymentId());
        assertEquals("CARD", response.paymentMethod());
        assertEquals("PAID", response.status());
        assertTrue(response.transactionReference().startsWith("TXN-"));
        verify(repository).save(any(Payment.class));
    }
}