package com.taxi.user.controller;

import com.taxi.user.model.Driver;
import com.taxi.user.repository.DriverRepository;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{id}/status")
    public Driver updateStatus(@PathVariable Long id, @RequestBody StatusUpdate status) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(status.getStatus());
        return repository.save(driver);
    }
}

class StatusUpdate {
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
