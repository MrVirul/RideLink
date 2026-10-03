package com.ridelink.driver_service;

import com.ridelink.driver_service.model.AvailabilityHistory;
import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.AvailabilityHistoryRepository;
import com.ridelink.driver_service.repository.DriverRepository;
import com.ridelink.driver_service.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverAvailabilityServiceTests {

    private static final Integer DRIVER_ID = 1;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AvailabilityHistoryRepository availabilityHistoryRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void togglesAvailabilityFromOfflineToOnlineAndUpdatesLastOnlineAtAndLogsHistory() {
        Driver driver = createSampleDriver(false);
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        Driver updated = driverService.toggleAvailability(DRIVER_ID, null);

        assertTrue(updated.isAvailable());
        assertNotNull(updated.getLastOnlineAt());
        assertTrue(updated.getLastOnlineAt().isAfter(beforeUpdate));
        verify(driverRepository).save(driver);

        ArgumentCaptor<AvailabilityHistory> captor = ArgumentCaptor.forClass(AvailabilityHistory.class);
        verify(availabilityHistoryRepository).save(captor.capture());
        AvailabilityHistory savedHistory = captor.getValue();
        assertEquals(DRIVER_ID, savedHistory.getDriverId());
        assertTrue(savedHistory.isAvailable());
        assertNotNull(savedHistory.getChangedAt());
    }

    @Test
    void togglesAvailabilityFromOnlineToOfflineAndLogsHistory() {
        Driver driver = createSampleDriver(true);
        LocalDateTime originalLastOnline = LocalDateTime.now().minusHours(1);
        driver.setLastOnlineAt(originalLastOnline);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Driver updated = driverService.toggleAvailability(DRIVER_ID, null);

        assertFalse(updated.isAvailable());
        assertEquals(originalLastOnline, updated.getLastOnlineAt());
        verify(driverRepository).save(driver);

        ArgumentCaptor<AvailabilityHistory> captor = ArgumentCaptor.forClass(AvailabilityHistory.class);
        verify(availabilityHistoryRepository).save(captor.capture());
        AvailabilityHistory savedHistory = captor.getValue();
        assertEquals(DRIVER_ID, savedHistory.getDriverId());
        assertFalse(savedHistory.isAvailable());
        assertNotNull(savedHistory.getChangedAt());
    }

    @Test
    void explicitUpdateAvailabilityOnlineSavesStateAndHistory() {
        Driver driver = createSampleDriver(false);
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Driver updated = driverService.updateAvailability(DRIVER_ID, true);

        assertTrue(updated.isAvailable());
        assertNotNull(updated.getLastOnlineAt());
        verify(driverRepository).save(driver);

        ArgumentCaptor<AvailabilityHistory> captor = ArgumentCaptor.forClass(AvailabilityHistory.class);
        verify(availabilityHistoryRepository).save(captor.capture());
        assertTrue(captor.getValue().isAvailable());
    }

    @Test
    void explicitUpdateAvailabilityOfflineSavesStateAndHistory() {
        Driver driver = createSampleDriver(true);
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Driver updated = driverService.updateAvailability(DRIVER_ID, false);

        assertFalse(updated.isAvailable());
        verify(driverRepository).save(driver);

        ArgumentCaptor<AvailabilityHistory> captor = ArgumentCaptor.forClass(AvailabilityHistory.class);
        verify(availabilityHistoryRepository).save(captor.capture());
        assertFalse(captor.getValue().isAvailable());
    }

    @Test
    void throwsExceptionWhenDriverNotFound() {
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> driverService.toggleAvailability(DRIVER_ID, null));

        assertEquals("Driver not found", ex.getMessage());
        verify(driverRepository, never()).save(any());
        verify(availabilityHistoryRepository, never()).save(any());
    }

    @Test
    void retrievesAvailabilityHistoryChronologically() {
        List<AvailabilityHistory> histories = List.of(
                new AvailabilityHistory(DRIVER_ID, true, LocalDateTime.now()),
                new AvailabilityHistory(DRIVER_ID, false, LocalDateTime.now().minusHours(2))
        );
        when(availabilityHistoryRepository.findByDriverIdOrderByChangedAtDesc(DRIVER_ID)).thenReturn(histories);

        List<AvailabilityHistory> result = driverService.getAvailabilityHistory(DRIVER_ID);

        assertEquals(2, result.size());
        verify(availabilityHistoryRepository).findByDriverIdOrderByChangedAtDesc(DRIVER_ID);
    }

    private Driver createSampleDriver(boolean available) {
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setUserId(25L);
        driver.setVehicleNumberPlate("CAB-1234");
        driver.setServiceArea("Colombo");
        driver.setAvailable(available);
        return driver;
    }
}
