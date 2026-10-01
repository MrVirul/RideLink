package com.ridelink.driver_service;

import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.DriverRepository;
import com.ridelink.driver_service.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationServiceTests {

    private static final Integer DRIVER_ID = 1;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void updatesLocationCoordinatesAccuracyAndTimestamp() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        Driver updated = driverService.updateLocation(DRIVER_ID, 6.9271, 79.8612, 10.5);

        assertEquals(6.9271, updated.getLatitude());
        assertEquals(79.8612, updated.getLongitude());
        assertEquals(10.5, updated.getAccuracy());
        assertNotNull(updated.getLastOnlineAt());
        org.junit.jupiter.api.Assertions.assertTrue(updated.getLastOnlineAt().isAfter(beforeUpdate));
        verify(driverRepository).save(driver);
    }

    @Test
    void defaultsAccuracyToZeroWhenNull() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Driver updated = driverService.updateLocation(DRIVER_ID, 6.9271, 79.8612, null);

        assertEquals(0.0, updated.getAccuracy());
        verify(driverRepository).save(driver);
    }

    @Test
    void rejectsInvalidLatitudeAboveNinety() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> driverService.updateLocation(DRIVER_ID, 95.0, 79.8612, 5.0));

        assertEquals("Coordinates out of Valid Range", thrown.getMessage());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidLatitudeBelowNegativeNinety() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> driverService.updateLocation(DRIVER_ID, -95.0, 79.8612, 5.0));

        assertEquals("Coordinates out of Valid Range", thrown.getMessage());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidLongitudeAboveOneEighty() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> driverService.updateLocation(DRIVER_ID, 6.9271, 185.0, 5.0));

        assertEquals("Coordinates out of Valid Range", thrown.getMessage());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidLongitudeBelowNegativeOneEighty() {
        Driver driver = createSampleDriver();
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> driverService.updateLocation(DRIVER_ID, 6.9271, -185.0, 5.0));

        assertEquals("Coordinates out of Valid Range", thrown.getMessage());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void throwsExceptionWhenDriverNotFound() {
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.empty());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> driverService.updateLocation(DRIVER_ID, 6.9271, 79.8612, 5.0));

        assertEquals("Driver not found", thrown.getMessage());
        verify(driverRepository, never()).save(any());
    }

    private Driver createSampleDriver() {
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setUserId(25L);
        driver.setVehicleNumberPlate("CAB-1234");
        driver.setServiceArea("Colombo");
        driver.setAvailable(true);
        return driver;
    }
}
