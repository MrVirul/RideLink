package com.ridelink.ride_service;

import com.ridelink.ride_service.Service.DriverAvailabilityService;
import com.ridelink.ride_service.client.AvailableDriver;
import com.ridelink.ride_service.client.DriverServiceClient;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAvailabilityServiceTests {

    @Mock
    private DriverServiceClient driverServiceClient;

    @InjectMocks
    private DriverAvailabilityService driverAvailabilityService;

    @Test
    void returnsDriversWhenDriverServiceHasSome() {
        List<AvailableDriver> available = List.of(
                new AvailableDriver(1, 5L, "WP-CAB-1234", 6.9271, 79.8612, "Colombo", true),
                new AvailableDriver(2, 6L, "WP-CAB-5678", 6.9345, 79.8428, "Colombo", true)
        );
        when(driverServiceClient.getAvailableDrivers()).thenReturn(available);

        assertEquals(2, driverAvailabilityService.findAvailableDrivers().size());
    }

    @Test
    void returnsEmptyListWhenNoDriverIsAvailable() {
        when(driverServiceClient.getAvailableDrivers()).thenReturn(List.of());

        assertTrue(driverAvailabilityService.findAvailableDrivers().isEmpty());
    }

    @Test
    void returnsEmptyListWhenDriverServiceSendsNoBody() {
        when(driverServiceClient.getAvailableDrivers()).thenReturn(null);

        assertTrue(driverAvailabilityService.findAvailableDrivers().isEmpty());
    }

    private static final String ELIGIBLE_PATH = "/api/v1/driver/eligible";

    @Test
    void propagatesFailureWhenDriverServiceIsDown() {
        Request request = Request.create(Request.HttpMethod.GET, ELIGIBLE_PATH,
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(driverServiceClient.getAvailableDrivers())
                .thenThrow(FeignException.errorStatus("driver-service is down",
                        Response.builder().status(503).reason("Service Unavailable").request(request).build()));

        assertThrows(FeignException.class, () -> driverAvailabilityService.findAvailableDrivers());
    }
}
