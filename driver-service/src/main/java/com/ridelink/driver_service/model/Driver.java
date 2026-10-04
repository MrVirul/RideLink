package com.ridelink.driver_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "drivers")
@Schema(name = "Driver", description = "A driver on the platform. Doubles as the registration request body and the response body for every driver endpoint.")
public class Driver {

    @Schema(description = "Id of the driver, assigned by the database, ignore it when registering", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Schema(description = "Id of the account this driver signs in with", example = "9")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Schema(description = "Registration number of the driver's vehicle, must be unique", example = "WP-CAB-4821")
    @Column(name = "Vehicle_Number_Plate", nullable = false, unique = true)
    private String vehicleNumberPlate;

    @Schema(description = "Current latitude of the driver", example = "6.9271")
    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Schema(description = "Current longitude of the driver", example = "79.8612")
    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Schema(description = "Area the driver is willing to operate in", example = "Colombo")
    @Column(name = "service_area", nullable = false)
    private String serviceArea;

    @Column(name = "availability")
    private boolean isAvailable = true;

    @Schema(description = "How accurate the reported position is, in metres", example = "12.5")
    @Column(name = "accuracy")
    private Double accuracy;

    @Schema(description = "When the driver was last online, set by the server", example = "2026-09-28T10:19:02", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;

    /**
     * Declared by hand rather than left to Lombok: springdoc names the property from this
     * getter ({@code isAvailable} becomes {@code available}), so the {@code @Schema} has to
     * sit here for the sample value to reach the published spec. Lombok skips generating a
     * getter that already exists, so {@code @Getter} on the class is unaffected.
     */
    @Schema(description = "Whether the driver is currently taking rides", example = "true")
    public boolean isAvailable() {
        return isAvailable;
    }
}