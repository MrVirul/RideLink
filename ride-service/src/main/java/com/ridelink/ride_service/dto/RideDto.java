package com.ridelink.ride_service.dto;

import com.ridelink.ride_service.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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

    @Schema(name = "CancelRideRequest")
    public static class CancelRideRequest {

        @Size(max = 500)
        @Schema(description = "Why the ride was cancelled, stored on the cancellation history row", example = "Change of plans")
        private String reason;

        public CancelRideRequest() {
        }

        public CancelRideRequest(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    @Schema(name = "CancellationResponse")
    public static class CancellationResponse {

        @Schema(description = "Id of the ride that was cancelled", example = "42")
        private Integer rideId;

        @Schema(description = "State the ride was in when it was cancelled", example = "SEARCHING")
        private Status previousStatus;

        @Schema(description = "Account id of whoever cancelled the ride", example = "1")
        private Long cancelledBy;

        @Schema(description = "When the ride was cancelled", example = "2026-09-28T10:22:10")
        private LocalDateTime cancelledAt;

        @Schema(description = "Reason given for the cancellation, null when none was given", example = "Change of plans")
        private String reason;

        public CancellationResponse() {
        }

        public CancellationResponse(Integer rideId, Status previousStatus, Long cancelledBy,
                                    LocalDateTime cancelledAt, String reason) {
            this.rideId = rideId;
            this.previousStatus = previousStatus;
            this.cancelledBy = cancelledBy;
            this.cancelledAt = cancelledAt;
            this.reason = reason;
        }

        public Integer getRideId() {
            return rideId;
        }

        public void setRideId(Integer rideId) {
            this.rideId = rideId;
        }

        public Status getPreviousStatus() {
            return previousStatus;
        }

        public void setPreviousStatus(Status previousStatus) {
            this.previousStatus = previousStatus;
        }

        public Long getCancelledBy() {
            return cancelledBy;
        }

        public void setCancelledBy(Long cancelledBy) {
            this.cancelledBy = cancelledBy;
        }

        public LocalDateTime getCancelledAt() {
            return cancelledAt;
        }

        public void setCancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
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

        @Schema(description = "When the ride was cancelled, null unless it was", example = "2026-09-28T10:22:10")
        private LocalDateTime cancelledAt;

        @Schema(description = "Account id of whoever cancelled the ride, null unless it was", example = "1")
        private Long cancelledBy;

        public RideRequestResponse() {
        }

        public RideRequestResponse(Integer id, Long passengerId, Long driverId, Status status,
                                   String pickupLocation, String dropOffLocation, double tripDistance,
                                   LocalDateTime requestedTime, LocalDateTime startTime, LocalDateTime completedTime) {
            this(id, passengerId, driverId, status, pickupLocation, dropOffLocation, tripDistance,
                    requestedTime, startTime, completedTime, null, null);
        }

        public RideRequestResponse(Integer id, Long passengerId, Long driverId, Status status,
                                   String pickupLocation, String dropOffLocation, double tripDistance,
                                   LocalDateTime requestedTime, LocalDateTime startTime, LocalDateTime completedTime,
                                   LocalDateTime cancelledAt, Long cancelledBy) {
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
            this.cancelledAt = cancelledAt;
            this.cancelledBy = cancelledBy;
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

        public LocalDateTime getCancelledAt() {
            return cancelledAt;
        }

        public void setCancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
        }

        public Long getCancelledBy() {
            return cancelledBy;
        }

        public void setCancelledBy(Long cancelledBy) {
            this.cancelledBy = cancelledBy;
        }
    }
}
