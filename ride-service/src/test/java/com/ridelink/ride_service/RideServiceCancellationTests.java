package com.ridelink.ride_service;

import com.ridelink.ride_service.Service.RideService;
import com.ridelink.ride_service.dto.RideDto;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.model.RideCancellation;
import com.ridelink.ride_service.model.Status;
import com.ridelink.ride_service.repository.RideCancellationRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceCancellationTests {

    private static final Long PASSENGER_ID = 7L;
    private static final Long DRIVER_ID = 8L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Integer RIDE_ID = 42;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideCancellationRepository rideCancellationRepository;

    @InjectMocks
    private RideService rideService;

    @Test
    void passengerCancelsSearchingRide() {
        stubRideIn(Status.SEARCHING, null);
        stubWrites();

        Ride cancelled = rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest("Change of plans"));

        assertEquals(Status.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals(PASSENGER_ID, cancelled.getCancelledBy());
    }

    @Test
    void passengerCancelsOngoingRide() {
        stubRideIn(Status.ONGOING, null);
        stubWrites();

        Ride cancelled = rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest());

        assertEquals(Status.CANCELLED, cancelled.getStatus());
    }

    @Test
    void recordsCancellationHistoryWithPreviousStatusAndReason() {
        stubRideIn(Status.ONGOING, null);
        stubWrites();

        rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest("Change of plans"));

        RideCancellation saved = captureHistory();
        assertEquals(RIDE_ID, saved.getRideId());
        assertEquals(Status.ONGOING, saved.getPreviousStatus());
        assertEquals(PASSENGER_ID, saved.getCancelledBy());
        assertNotNull(saved.getCancelledAt());
        assertEquals("Change of plans", saved.getReason());
    }

    @Test
    void recordsNullReasonWhenNoBodyIsSent() {
        stubRideIn(Status.SEARCHING, null);
        stubWrites();

        rideService.cancelRide(RIDE_ID, PASSENGER_ID, null);

        assertNull(captureHistory().getReason());
    }

    @Test
    void rejectsCompletedRide() {
        stubRideIn(Status.COMPLETED, null);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest()));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideCancellationRepository, never()).save(any());
    }

    @Test
    void rejectsAlreadyCancelledRide() {
        stubRideIn(Status.CANCELLED, null);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest()));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideCancellationRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownRide() {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest()));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void rejectsCallerWhoIsNeitherPassengerNorDriver() {
        stubRideIn(Status.SEARCHING, DRIVER_ID);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.cancelRide(RIDE_ID, OUTSIDER_ID, new RideDto.CancelRideRequest()));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
        verify(rideRepository, never()).save(any());
        verify(rideCancellationRepository, never()).save(any());
    }

    @Test
    void readsRideUnderPessimisticLock() {
        stubRideIn(Status.SEARCHING, null);
        stubWrites();

        rideService.cancelRide(RIDE_ID, PASSENGER_ID, new RideDto.CancelRideRequest());

        verify(rideRepository).findByIdForUpdate(RIDE_ID);
    }

    @Test
    void assignedDriverCancelsSearchingRide() {
        stubRideIn(Status.SEARCHING, DRIVER_ID);
        stubWrites();

        Ride cancelled = rideService.cancelRide(RIDE_ID, DRIVER_ID, new RideDto.CancelRideRequest());

        assertEquals(Status.CANCELLED, cancelled.getStatus());
        assertEquals(DRIVER_ID, cancelled.getCancelledBy());
    }

    @Test
    void driverCannotCancelOngoingRide() {
        stubRideIn(Status.ONGOING, DRIVER_ID);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.cancelRide(RIDE_ID, DRIVER_ID, new RideDto.CancelRideRequest()));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(rideRepository, never()).save(any());
        verify(rideCancellationRepository, never()).save(any());
    }

    @Test
    void assignedDriverCanReadItsOwnCancellation() {
        stubRideRead(Status.SEARCHING, DRIVER_ID);
        RideCancellation recorded = new RideCancellation();
        recorded.setRideId(RIDE_ID);
        recorded.setPreviousStatus(Status.SEARCHING);
        recorded.setCancelledBy(DRIVER_ID);
        recorded.setCancelledAt(LocalDateTime.now());
        when(rideCancellationRepository.findByRideId(RIDE_ID)).thenReturn(Optional.of(recorded));

        RideCancellation found = rideService.getCancellation(RIDE_ID, DRIVER_ID);

        assertEquals(DRIVER_ID, found.getCancelledBy());
    }

    @Test
    void outsiderCannotReadCancellation() {
        stubRideRead(Status.SEARCHING, DRIVER_ID);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.getCancellation(RIDE_ID, OUTSIDER_ID));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    @Test
    void readingCancellationOfUncancelledRideIsNotFound() {
        stubRideRead(Status.SEARCHING, DRIVER_ID);
        when(rideCancellationRepository.findByRideId(RIDE_ID)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> rideService.getCancellation(RIDE_ID, DRIVER_ID));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    private void stubRideIn(Status status, Long driverId) {
        when(rideRepository.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride(status, driverId)));
    }

    private void stubRideRead(Status status, Long driverId) {
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride(status, driverId)));
    }

    private void stubWrites() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideCancellationRepository.save(any(RideCancellation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ride ride(Status status, Long driverId) {
        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setUserId(PASSENGER_ID);
        ride.setStatus(status);
        ride.setDriverId(driverId);
        ride.setPickupLocation("Colombo Fort Railway Station");
        ride.setDropOffLocation("Galle Face Green");
        ride.setTripDistance(7.4);
        return ride;
    }

    private RideCancellation captureHistory() {
        ArgumentCaptor<RideCancellation> captor = ArgumentCaptor.forClass(RideCancellation.class);
        verify(rideCancellationRepository).save(captor.capture());
        RideCancellation saved = captor.getValue();
        assertNotNull(saved);
        return saved;
    }
}
