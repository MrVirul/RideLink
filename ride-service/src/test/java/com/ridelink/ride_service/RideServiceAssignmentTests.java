package com.ridelink.ride_service;

import com.ridelink.ride_service.Service.DriverAvailabilityService;
import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.client.AvailableDriver;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.repository.RideCancellationRepository;
import com.ridelink.ride_service.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceAssignmentTests {

    private static final Integer RIDE_ID = 42;
    private static final Long DRIVER_ACCOUNT_ID = 9L;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideCancellationRepository rideCancellationRepository;

    @Mock
    private DriverAvailabilityService driverAvailabilityService;

    @InjectMocks
    private RideService rideService;

    @Test
    void assignsTheFirstAvailableDriver() {
        stubSearchingRide();
        when(driverAvailabilityService.findAvailableDrivers())
                .thenReturn(List.of(driver(DRIVER_ACCOUNT_ID), driver(10L)));
        stubSave();

        Ride assigned = rideService.assignDriver(RIDE_ID);

        assertEquals(DRIVER_ACCOUNT_ID, assigned.getDriverId(),
                "driver_id must hold the driver's account id, which is what cancel compares against");
        assertEquals(Status.ASSIGNED, assigned.getStatus());
    }

    @Test
    void stampsAssignedAtServerSide() {
        stubSearchingRide();
        when(driverAvailabilityService.findAvailableDrivers()).thenReturn(List.of(driver(DRIVER_ACCOUNT_ID)));
        stubSave();
        LocalDateTime before = LocalDateTime.now();

        Ride assigned = rideService.assignDriver(RIDE_ID);

        assertNotNull(assigned.getAssignedAt());
        assertTrue(!assigned.getAssignedAt().isBefore(before), "assigned_at should be stamped now");
    }

    @Test
    void leavesRideStartTimeUnset() {
        stubSearchingRide();
        when(driverAvailabilityService.findAvailableDrivers()).thenReturn(List.of(driver(DRIVER_ACCOUNT_ID)));
        stubSave();

        Ride assigned = rideService.assignDriver(RIDE_ID);

        assertNull(assigned.getStartTime(), "the ride has not started yet");
        assertNull(assigned.getCancelledAt());
    }

    @Test
    void rejectsUnknownRide() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(RIDE_ID));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
        verify(driverAvailabilityService, never()).findAvailableDrivers();
    }

    @Test
    void rejectsAssignmentOfAnAlreadyAssignedRide() {
        Ride ride = rideInState(Status.ASSIGNED);
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(RIDE_ID));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(driverAvailabilityService, never()).findAvailableDrivers();
        verify(rideRepository, never()).save(any());
    }

    @Test
    void rejectsAssignmentOfAnOngoingRide() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(rideInState(Status.ONGOING)));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(RIDE_ID));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideRepository, never()).save(any());
    }

    @Test
    void rejectsAssignmentOfACancelledRide() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(rideInState(Status.CANCELLED)));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(RIDE_ID));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideRepository, never()).save(any());
    }

    @Test
    void leavesTheRideSearchingWhenNoDriverIsAvailable() {
        stubSearchingRide();
        when(driverAvailabilityService.findAvailableDrivers()).thenReturn(List.of());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.assignDriver(RIDE_ID));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        assertTrue(thrown.getReason().contains("No drivers"), thrown.getReason());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void locksTheRideBeforeAssigning() {
        stubSearchingRide();
        when(driverAvailabilityService.findAvailableDrivers()).thenReturn(List.of(driver(DRIVER_ACCOUNT_ID)));
        stubSave();

        rideService.assignDriver(RIDE_ID);

        verify(rideRepository).findByIdForUpdate(RIDE_ID);
        verify(rideRepository, never()).findById(anyInt());
    }

    @Test
    void letsTheAssignedDriverCancelTheRide() {
        Ride ride = rideInState(Status.ASSIGNED);
        ride.setDriverId(DRIVER_ACCOUNT_ID);
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideCancellationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Ride cancelled = rideService.cancelRide(RIDE_ID, DRIVER_ACCOUNT_ID, null);

        assertEquals(Status.CANCELLED, cancelled.getStatus());
        assertEquals(DRIVER_ACCOUNT_ID, cancelled.getCancelledBy());
    }

    private void stubSearchingRide() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(rideInState(Status.SEARCHING)));
    }

    private void stubSave() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ride rideInState(Status status) {
        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setUserId(7L);
        ride.setStatus(status);
        ride.setPickupLocation("Colombo Fort Railway Station");
        ride.setDropOffLocation("Galle Face Green");
        ride.setTripDistance(7.4);
        return ride;
    }

    private AvailableDriver driver(Long accountId) {
        return new AvailableDriver(accountId.intValue(), accountId, "WP-CAB-" + accountId,
                6.9271, 79.8612, "Colombo", true);
    }
}