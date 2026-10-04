package com.ridelink.fare_service.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare_service.dto.PaymentRequest;
import com.ridelink.fare_service.dto.PaymentResponse;
import com.ridelink.fare_service.dto.ReceiptResponse;
import com.ridelink.fare_service.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Create a simulated payment")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.created(URI.create("/api/payments/" + response.paymentId())).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "List payments for a ride")
    public ResponseEntity<List<PaymentResponse>> getPaymentsForRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(paymentService.getPaymentsForRide(rideId));
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get a payment receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getReceipt(paymentId));
    }
}