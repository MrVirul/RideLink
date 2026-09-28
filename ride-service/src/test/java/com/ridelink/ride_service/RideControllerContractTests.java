package com.ridelink.ride_service;

import com.ridelink.ride_service.controller.RideController;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.Service.RideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
class RideControllerContractTests {

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
    void cancelWithNoBodyIsAccepted() throws Exception {
        when(rideService.cancelRide(anyInt(), anyLong(), any())).thenReturn(new Ride());

        mockMvc.perform(post("/api/v1/ride/1/cancel").header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsCreated() throws Exception {
        Ride ride = new Ride();
        ride.setId(42);
        ride.setStatus(Status.SEARCHING);
        when(rideService.createRideRequest(anyLong(), any())).thenReturn(ride);

        mockMvc.perform(post("/api/v1/ride/request")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"a\",\"dropOffLocation\":\"b\",\"tripDistance\":7.4}"))
                .andExpect(status().isCreated());
    }
}
