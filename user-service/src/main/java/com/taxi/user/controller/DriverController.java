package com.taxi.user.controller;

import com.taxi.user.model.Driver;
import com.taxi.user.repository.DriverRepository;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {
    private final DriverRepository repository;

    public DriverController(DriverRepository repository) {
        this.repository = repository;
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

    @PutMapping("/{id}/status")
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

    @Data
    static class StatusUpdate {
        private String status;
    }
}
