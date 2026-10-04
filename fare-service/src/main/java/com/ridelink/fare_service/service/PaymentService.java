package com.ridelink.fare_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridelink.fare_service.dto.PaymentRequest;
import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.dto.ReceiptResponse;
import com.ridelink.fare_service.entity.Payment;
import com.ridelink.fare_service.exception.ResourceNotFoundException;
import com.ridelink.fare_service.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(PaymentRequest request) {
        Payment payment = new Payment(
                request.rideId(),
                request.amount(),
                request.paymentMethod().toUpperCase(Locale.ROOT),
                "PAID",
                "TXN-" + UUID.randomUUID(),
                LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse getPayment(Long paymentId) {
        return toResponse(findPayment(paymentId));
    }

    public List<PaymentResponse> getPaymentsForRide(Long rideId) {
        return paymentRepository.findByRideIdOrderByPaidAtDesc(rideId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReceiptResponse getReceipt(Long paymentId) {
        Payment payment = findPayment(paymentId);
        return new ReceiptResponse(
                "RCP-" + payment.getId(),
                payment.getId(),
                payment.getRideId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getPaidAt(),
                "PAID".equals(payment.getStatus())
                        ? "Payment completed successfully"
                        : "Payment was not completed");
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " was not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRideId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getPaidAt());
    }
}