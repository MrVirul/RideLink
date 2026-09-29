package com.ridelink.ride_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ride_cancellations",
        uniqueConstraints = @UniqueConstraint(name = "uk_ride_cancellations_ride_id", columnNames = "ride_id")
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "RideCancellation", description = "Audit row written when a ride is cancelled, unique per ride. Never serialised directly: GET /api/v1/ride/{id}/cancellation returns the CancellationResponse projection.")
public class RideCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ride_id", nullable = false)
    private Integer rideId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private Status previousStatus;

    @Column(name = "cancelled_by", nullable = false)
    private Long cancelledBy;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    @Column(name = "reason", length = 500)
    private String reason;
}
