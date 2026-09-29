package com.ridelink.ride_service;

import com.ridelink.ride_service.controller.RideController;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.RideCancellation;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.Service.RideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
class RideControllerContractTests {

    private static final Long PASSENGER_ID = 7L;
    private static final Integer RIDE_ID = 42;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void nonPositiveHeaderIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ride/1/cancel").header("X-User-Id", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingHeaderIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ride/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankPickupIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ride/request")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"\",\"dropOffLocation\":\"x\",\"tripDistance\":1.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tooLongReasonIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ride/1/cancel")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsCreatedWithLocationHeader() throws Exception {
        when(rideService.createRideRequest(anyLong(), any())).thenReturn(searchingRide());

        mockMvc.perform(post("/api/v1/ride/request")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"a\",\"dropOffLocation\":\"b\",\"tripDistance\":7.4}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/ride/42")));
    }

    @Test
    void createReturnsThePersistedRide() throws Exception {
        when(rideService.createRideRequest(anyLong(), any())).thenReturn(searchingRide());

        mockMvc.perform(post("/api/v1/ride/request")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"a\",\"dropOffLocation\":\"b\",\"tripDistance\":7.4}"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.passengerId").value(PASSENGER_ID))
                .andExpect(jsonPath("$.status").value("SEARCHING"))
                .andExpect(jsonPath("$.tripDistance").value(7.4))
                .andExpect(content().string(containsString("\"driverId\":null")))
                .andExpect(content().string(containsString("\"cancelledAt\":null")));
    }

    @Test
    void createConflictsWhenPassengerAlreadyHasAnActiveRide() throws Exception {
        when(rideService.createRideRequest(anyLong(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already has ride 11"));

        mockMvc.perform(post("/api/v1/ride/request")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"a\",\"dropOffLocation\":\"b\",\"tripDistance\":7.4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getRideReturnsTheRide() throws Exception {
        when(rideService.getRideForPassenger(RIDE_ID, PASSENGER_ID)).thenReturn(searchingRide());

        mockMvc.perform(get("/api/v1/ride/42").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("SEARCHING"));
    }

    @Test
    void getRideIsNotFoundForUnknownId() throws Exception {
        when(rideService.getRideForPassenger(anyInt(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride 99 not found"));

        mockMvc.perform(get("/api/v1/ride/99").header("X-User-Id", "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelReturnsTheCancelledRide() throws Exception {
        when(rideService.cancelRide(anyInt(), anyLong(), any())).thenReturn(cancelledRide());

        mockMvc.perform(post("/api/v1/ride/1/cancel").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value(PASSENGER_ID))
                .andExpect(content().string(containsString("cancelledAt")));
    }

    @Test
    void cancelSurfacesConflictFromTheService() throws Exception {
        when(rideService.cancelRide(anyInt(), anyLong(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "cannot be cancelled from state COMPLETED"));

        mockMvc.perform(post("/api/v1/ride/1/cancel").header("X-User-Id", "1"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelSurfacesForbiddenFromTheService() throws Exception {
        when(rideService.cancelRide(anyInt(), anyLong(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "not part of ride 1"));

        mockMvc.perform(post("/api/v1/ride/1/cancel").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellationReturnsTheAuditRow() throws Exception {
        when(rideService.getCancellation(RIDE_ID, PASSENGER_ID)).thenReturn(cancellation());

        mockMvc.perform(get("/api/v1/ride/42/cancellation").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(RIDE_ID))
                .andExpect(jsonPath("$.previousStatus").value("SEARCHING"))
                .andExpect(jsonPath("$.cancelledBy").value(PASSENGER_ID))
                .andExpect(jsonPath("$.reason").value("Change of plans"));
    }

    @Test
    void cancellationIsNotFoundWhenTheRideWasNeverCancelled() throws Exception {
        when(rideService.getCancellation(anyInt(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "was never cancelled"));

        mockMvc.perform(get("/api/v1/ride/42/cancellation").header("X-User-Id", "7"))
                .andExpect(status().isNotFound());
    }

    private Ride searchingRide() {
        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setUserId(PASSENGER_ID);
        ride.setStatus(Status.SEARCHING);
        ride.setPickupLocation("Colombo Fort Railway Station");
        ride.setDropOffLocation("Galle Face Green");
        ride.setTripDistance(7.4);
        ride.setRequestedTime(LocalDateTime.now());
        return ride;
    }

    private Ride cancelledRide() {
        Ride ride = searchingRide();
        ride.setStatus(Status.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancelledBy(PASSENGER_ID);
        return ride;
    }

    private RideCancellation cancellation() {
        RideCancellation cancellation = new RideCancellation();
        cancellation.setRideId(RIDE_ID);
        cancellation.setPreviousStatus(Status.SEARCHING);
        cancellation.setCancelledBy(PASSENGER_ID);
        cancellation.setCancelledAt(LocalDateTime.now());
        cancellation.setReason("Change of plans");
        return cancellation;
    }
}
