package com.ridelink.driver_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "availability_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AvailabilityHistory", description = "Audit trail recording driver availability state changes (online/offline).")
public class AvailabilityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "driver_id", nullable = false)
    private Integer driverId;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public AvailabilityHistory(Integer driverId, boolean isAvailable, LocalDateTime changedAt) {
        this.driverId = driverId;
        this.isAvailable = isAvailable;
        this.changedAt = changedAt;
    }
}
