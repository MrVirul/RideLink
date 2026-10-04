package com.ridelink.driver_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;

public class DriverDto {

    @Schema(name = "LocationRequest")
    public static class LocationRequest {
        @Schema(description = "Current latitude of the driver, between -90 and 90", example = "6.9271")
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
        private double latitude;

        @Schema(description = "Current longitude of the driver, between -180 and 180", example = "79.8612")
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
        private double longitude;

        public LocationRequest() {
        }

        public LocationRequest(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        @Schema(description = "How accurate the reported position is, in metres", example = "12.5")
        private Double accuracy;

        public Double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Double accuracy) {
            this.accuracy = accuracy;
        }
    }

    @Schema(name = "AvailabilityRequest")
    public static class AvailabilityRequest {

        @Schema(description = "Whether the driver is taking rides, omit to flip the current value", example = "true")
        private Boolean available;

        public AvailabilityRequest() {
        }

        public AvailabilityRequest(Boolean available) {
            this.available = available;
        }

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }
}