package com.taxi.trip.controller;

import com.taxi.trip.model.Trip;
import com.taxi.trip.repository.TripRepository;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {
    private final TripRepository repository;

    public TripController(TripRepository repository) {
        this.repository = repository;
    }

    @Data
    static class TripRequest {
        private Long passengerId;
        private String origin;
        private String destination;
    }

    @PostMapping
    public Trip create(@RequestBody TripRequest request) {
        Trip trip = new Trip();
        trip.setPassengerId(request.getPassengerId());
        trip.setOrigin(request.getOrigin());
        trip.setDestination(request.getDestination());
        trip.setStatus("CREATED");
        return repository.save(trip);
    }

    @GetMapping("/{id}")
    public Trip getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }

    @GetMapping
    public List<Trip> getByPassengerId(@RequestParam("passenger_id") Long passengerId) {
        return repository.findByPassengerId(passengerId);
    }

    @Data
    static class StatusUpdate {
        private String status;
    }

    @PatchMapping("/{id}/status")
    public Trip updateStatus(@PathVariable Long id, @RequestBody StatusUpdate status) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus(status.getStatus());
        trip.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(trip);
    }
}
