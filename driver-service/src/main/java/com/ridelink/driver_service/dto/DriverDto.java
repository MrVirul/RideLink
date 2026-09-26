package com.ridelink.driver_service.dto;


public class DriverDto {

    public static class LocationRequest {

        private Double latitude;
        private Double longitude;

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
    }

    public static class AvailabilityRequest {

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
