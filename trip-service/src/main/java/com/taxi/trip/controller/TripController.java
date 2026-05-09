package com.taxi.trip.controller;

import com.taxi.trip.model.Trip;
import com.taxi.trip.repository.TripRepository;
import com.taxi.trip.service.TripService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {
    private final TripRepository repository;
    private final TripService tripService;

    public TripController(TripRepository repository, TripService tripService) {
        this.repository = repository;
        this.tripService = tripService;
    }

    @PostMapping
    public Trip create(@RequestBody TripRequest request) {
        return tripService.createTrip(
                request.getPassengerId(),
                request.getOrigin(),
                request.getDestination(),
                request.getDistance(),
                request.getTariffType()
        );
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

    @PatchMapping("/{id}/status")
    public Trip updateStatus(@PathVariable Long id, @RequestBody StatusUpdate status) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setStatus(status.getStatus());
        trip.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(trip);
    }

    @PatchMapping("/{id}/driver")
    public Trip updateDriver(@PathVariable Long id, @RequestBody DriverUpdate update) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        trip.setDriverId(update.getDriverId());
        trip.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(trip);
    }

    @Data
    static class DriverUpdate {
        private Long driverId;
    }

    @Data
    static class TripRequest {
        private Long passengerId;
        private String origin;
        private String destination;
        private Double distance;
        private String tariffType;
    }

    @Data
    static class StatusUpdate {
        private String status;
    }
}
