package com.ridelink.fare_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridelink.fare_service.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByRideIdOrderByPaidAtDesc(Long rideId);
}