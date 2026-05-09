package com.taxi.user.controller;

import com.taxi.user.model.Driver;
import com.taxi.user.repository.DriverRepository;
import com.taxi.user.service.DriverService;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {
    private final DriverRepository repository;

    private final DriverService driverService;

    public DriverController(DriverRepository repository,
                            DriverService driverService) {
        this.repository = repository;
        this.driverService = driverService;
    }

    @PostMapping
    public Driver create(@RequestBody Driver driver) {
        return repository.save(driver);
    }

    @GetMapping("/{id}")
    public Driver getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
    }

    @PatchMapping("/{id}/status")
    public Driver updateStatus(@PathVariable Long id, @RequestBody StatusUpdate request) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(request.getStatus());
        return repository.save(driver);
    }

    @GetMapping("/available")
    public List<Driver> getAvailableDrivers() {
        return repository.findByStatus("FREE");
    }

    @PostMapping("/assign")
    public Driver assignFirstAvailable() {
        return driverService.assignFirstAvailable();
    }

    @Data
    static class StatusUpdate {
        private String status;
    }
}
