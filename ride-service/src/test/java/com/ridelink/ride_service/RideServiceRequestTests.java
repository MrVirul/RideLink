package com.ridelink.ride_service;

import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceRequestTests {

    private static final Long PASSENGER_ID = 7L;
    private static final Long OTHER_PASSENGER_ID = 99L;
    private static final Integer RIDE_ID = 42;

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    @Test
    void createsSearchingRideOwnedByTheCallingPassenger() {
        LocalDateTime before = LocalDateTime.now();
        stubSave();

        Ride created = rideService.createRideRequest(PASSENGER_ID, request());

        assertEquals(PASSENGER_ID, created.getUserId());
        assertEquals(Status.SEARCHING, created.getStatus());
        assertNotNull(created.getRequestedTime());
        assertFalse(created.getRequestedTime().isBefore(before),
                "requestedTime should be stamped by the server, not backdated");
    }

    @Test
    void mapsLocationsAndDistanceFromTheRequest() {
        stubSave();

        Ride created = rideService.createRideRequest(PASSENGER_ID, request());

        assertEquals("Colombo Fort Railway Station", created.getPickupLocation());
        assertEquals("Galle Face Green", created.getDropOffLocation());
        assertEquals(7.4, created.getTripDistance());
    }

    @Test
    void leavesDriverAndLifecycleFieldsUnset() {
        stubSave();

        Ride created = rideService.createRideRequest(PASSENGER_ID, request());

        assertNull(created.getId());
        assertNull(created.getDriverId());
        assertNull(created.getStartTime());
        assertNull(created.getCompletedTime());
        assertNull(created.getCancelledAt());
        assertNull(created.getCancelledBy());
    }

    @Test
    void persistsTheRideItBuilds() {
        rideService.createRideRequest(PASSENGER_ID, request());

        Ride saved = captureSavedRide();
        assertEquals("Colombo Fort Railway Station", saved.getPickupLocation());
        assertEquals(Status.SEARCHING, saved.getStatus());
    }

    @Test
    void rejectsSecondRideWhileOneIsSearching() {
        stubActiveRide(Status.SEARCHING, 11);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.createRideRequest(PASSENGER_ID, request()));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        assertTrue(thrown.getReason().contains("11"), "the message should name the conflicting ride");
        verify(rideRepository, never()).save(any());
    }

    @Test
    void rejectsSecondRideWhileOneIsOngoing() {
        stubActiveRide(Status.ONGOING, 11);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.createRideRequest(PASSENGER_ID, request()));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideRepository, never()).save(any());
    }

    @Test
    void blocksOnlySearchingAndOngoingRides() {
        ArgumentCaptor<Collection<Status>> captor = ArgumentCaptor.forClass(Collection.class);
        rideService.createRideRequest(PASSENGER_ID, request());

        verify(rideRepository).findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(
                eq(PASSENGER_ID), captor.capture());
        assertEquals(List.of(Status.SEARCHING, Status.ONGOING), captor.getValue(),
                "SEARCHING and ONGOING are the only statuses that should block a new request");
    }

    @Test
    void onlyLooksForTheCallingPassengersActiveRide() {
        rideService.createRideRequest(PASSENGER_ID, request());

        verify(rideRepository).findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(
                eq(PASSENGER_ID), any());
    }

    @Test
    void returnsRideOwnedByThePassenger() {
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride(Status.SEARCHING, PASSENGER_ID)));

        Ride found = rideService.getRideForPassenger(RIDE_ID, PASSENGER_ID);

        assertEquals(RIDE_ID, found.getId());
        assertEquals(Status.SEARCHING, found.getStatus());
    }

    @Test
    void rejectsUnknownRide() {
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.getRideForPassenger(RIDE_ID, PASSENGER_ID));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void rejectsRideOwnedByAnotherPassenger() {
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride(Status.SEARCHING, OTHER_PASSENGER_ID)));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.getRideForPassenger(RIDE_ID, PASSENGER_ID));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    private void stubSave() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubActiveRide(Status status, Integer id) {
        Ride active = ride(status, PASSENGER_ID);
        active.setId(id);
        when(rideRepository.findFirstByUserIdAndStatusInOrderByRequestedTimeDesc(anyLong(), any()))
                .thenReturn(Optional.of(active));
    }

    private Ride captureSavedRide() {
        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(captor.capture());
        return captor.getValue();
    }

    private Ride ride(Status status, Long passengerId) {
        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setUserId(passengerId);
        ride.setStatus(status);
        ride.setPickupLocation("Colombo Fort Railway Station");
        ride.setDropOffLocation("Galle Face Green");
        ride.setTripDistance(7.4);
        return ride;
    }

    private RideDto.RideRequest request() {
        return new RideDto.RideRequest("Colombo Fort Railway Station", "Galle Face Green", 7.4);
    }
}
