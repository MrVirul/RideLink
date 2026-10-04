package com.ridelink.fare_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare_service.dto.FareEstimateRequest;
import com.ridelink.fare_service.dto.FareEstimateResponse;
import com.ridelink.fare_service.dto.FinalFareRequest;
import com.ridelink.fare_service.dto.FinalFareResponse;
import com.ridelink.fare_service.service.FareService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fare")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate a ride fare")
    public ResponseEntity<FareEstimateResponse> estimateFare(
            @Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.calculateEstimate(request));
    }

    @PostMapping("/final")
    @Operation(summary = "Calculate the final fare for a completed ride")
    public ResponseEntity<FinalFareResponse> calculateFinalFare(
            @Valid @RequestBody FinalFareRequest request) {
        return ResponseEntity.ok(fareService.calculateFinalFare(request));
    }
}