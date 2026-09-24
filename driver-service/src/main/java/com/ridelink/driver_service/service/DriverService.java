package com.ridelink.driver_service.service;

import com.ridelink.driver_service.model.Driver;
import com.ridelink.driver_service.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    public Driver reigsterDriver(Driver driver){
        driver.setAvailable(true);
        return driverRepository.save(driver);
    }

    public Driver updateLocation(Integer id, double lattitude, double longtitute){
        Driver driver = driverRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Driver not found"));
        driver.setLatitude(lattitude);
        driver.setLongitude(longtitute);
        return driverRepository.save(driver);
    }

    public Driver updateAvailability(Integer id,boolean isAvailable){
        Driver driver = driverRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Driver not found"));
        driver.setAvailable(isAvailable);
        return driverRepository.save(driver);
    }

    public List<Driver> getEligibleAvailableDrivers(String serviceArea){
        if(serviceArea != null && !serviceArea.isEmpty()){
            return driverRepository.findByIsAvailableAndServiceArea(true,serviceArea);
        }
        return driverRepository.findByIsAvailable(true);
    }
}