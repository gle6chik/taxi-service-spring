package com.taxi.user.service;

import com.taxi.user.model.Driver;
import com.taxi.user.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {
    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Transactional
    public Driver assignFirstAvailable() {
        List<Driver> freeDrivers = driverRepository.findFreeDriversWithLock();
        if (freeDrivers.isEmpty()) {
            throw new RuntimeException("No available drivers");
        }
        Driver driver = freeDrivers.get(0);
        driver.setStatus("BUSY");
        return driverRepository.save(driver);
    }
}
