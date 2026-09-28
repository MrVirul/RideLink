package com.ridelink.ride_service.dto;

import com.ridelink.ride_service.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class RideDto {

    @Schema(name = "RideRequest")
    public static class RideRequest {

        @NotBlank
        @Schema(description = "Where the passenger wants to be picked up", example = "Colombo Fort Railway Station")
        private String pickupLocation;

        @NotBlank
        @Schema(description = "Where the passenger wants to be dropped off", example = "Galle Face Green")
        private String dropOffLocation;

        @Positive
        @Schema(description = "Trip distance in kilometres, as quoted to the passenger", example = "7.4")
        private double tripDistance;

        public RideRequest() {
        }

        public RideRequest(String pickupLocation, String dropOffLocation, double tripDistance) {
            this.pickupLocation = pickupLocation;
            this.dropOffLocation = dropOffLocation;
            this.tripDistance = tripDistance;
        }

        public String getPickupLocation() {
            return pickupLocation;
        }

        public void setPickupLocation(String pickupLocation) {
            this.pickupLocation = pickupLocation;
        }

        public String getDropOffLocation() {
            return dropOffLocation;
        }

        public void setDropOffLocation(String dropOffLocation) {
            this.dropOffLocation = dropOffLocation;
        }

        public double getTripDistance() {
            return tripDistance;
        }

        public void setTripDistance(double tripDistance) {
            this.tripDistance = tripDistance;
        }
    }

    @Schema(name = "RideRequestResponse")
    public static class RideRequestResponse {

        @Schema(description = "Id of the created ride, used to track it afterwards", example = "42")
        private Integer id;

        @Schema(description = "Account id of the passenger who requested the ride", example = "1")
        private Long passengerId;

        @Schema(description = "Account id of the matched driver, null while searching", example = "9")
        private Long driverId;

        @Schema(description = "Lifecycle state of the ride, SEARCHING on creation", example = "SEARCHING")
        private Status status;

        @Schema(description = "Where the passenger wants to be picked up", example = "Colombo Fort Railway Station")
        private String pickupLocation;

        @Schema(description = "Where the passenger wants to be dropped off", example = "Galle Face Green")
        private String dropOffLocation;

        @Schema(description = "Trip distance in kilometres, as quoted to the passenger", example = "7.4")
        private double tripDistance;

        @Schema(description = "Server-side timestamp of when the ride was requested", example = "2026-09-28T10:15:30")
        private LocalDateTime requestedTime;

        @Schema(description = "When the ride started, null while searching", example = "2026-09-28T10:19:02")
        private LocalDateTime startTime;

        @Schema(description = "When the ride completed, null until it does", example = "2026-09-28T10:41:55")
        private LocalDateTime completedTime;

        public RideRequestResponse() {
        }

        public RideRequestResponse(Integer id, Long passengerId, Long driverId, Status status,
                                   String pickupLocation, String dropOffLocation, double tripDistance,
                                   LocalDateTime requestedTime, LocalDateTime startTime, LocalDateTime completedTime) {
            this.id = id;
            this.passengerId = passengerId;
            this.driverId = driverId;
            this.status = status;
            this.pickupLocation = pickupLocation;
            this.dropOffLocation = dropOffLocation;
            this.tripDistance = tripDistance;
            this.requestedTime = requestedTime;
            this.startTime = startTime;
            this.completedTime = completedTime;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Long getPassengerId() {
            return passengerId;
        }

        public void setPassengerId(Long passengerId) {
            this.passengerId = passengerId;
        }

        public Long getDriverId() {
            return driverId;
        }

        public void setDriverId(Long driverId) {
            this.driverId = driverId;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getPickupLocation() {
            return pickupLocation;
        }

        public void setPickupLocation(String pickupLocation) {
            this.pickupLocation = pickupLocation;
        }

        public String getDropOffLocation() {
            return dropOffLocation;
        }

        public void setDropOffLocation(String dropOffLocation) {
            this.dropOffLocation = dropOffLocation;
        }

        public double getTripDistance() {
            return tripDistance;
        }

        public void setTripDistance(double tripDistance) {
            this.tripDistance = tripDistance;
        }

        public LocalDateTime getRequestedTime() {
            return requestedTime;
        }

        public void setRequestedTime(LocalDateTime requestedTime) {
            this.requestedTime = requestedTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getCompletedTime() {
            return completedTime;
        }

        public void setCompletedTime(LocalDateTime completedTime) {
            this.completedTime = completedTime;
        }
    }
}
