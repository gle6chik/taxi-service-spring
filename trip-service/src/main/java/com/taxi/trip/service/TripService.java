package com.taxi.trip.service;

import com.taxi.trip.model.Trip;
import com.taxi.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://localhost:8081";

    public TripService(TripRepository tripRepository, RestTemplate restTemplate) {
        this.tripRepository = tripRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Trip createTrip(Long passengerId, String origin, String destination) {
        // Checking passenger existing
        checkPassengerExists(passengerId);

        // Search free driver
        Long driverId = findAvailableDriver();
        if (driverId == null) {
            throw new RuntimeException("No available drivers");
        }

        // Create trip
        Trip trip = new Trip();
        trip.setPassengerId(passengerId);
        trip.setDriverId(driverId);
        trip.setOrigin(origin);
        trip.setDestination(destination);
        trip.setStatus("DRIVER_ASSIGNED");
        trip.setPrice(calculatePrice(origin, destination));

        Trip savedTrip = tripRepository.save(trip);

        // Update driver status to BUSY
        updateDriverStatus(driverId, "BUSY");

        return savedTrip;
    }

    private void checkPassengerExists(Long passengerId) {
        try {
            restTemplate.getForObject(USER_SERVICE_URL + "/passengers/" + passengerId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Passenger not found: " + passengerId);
        }
    }

    private Long findAvailableDriver() {
        // TODO: get free drivers list
        return 1L; // mock
    }

    private Double calculatePrice(String origin, String destination) {
        // TODO: calculate trip price (distance * tariff)
        return 1000D; // Mock
    }

    private void updateDriverStatus(Long driverId, String status) {
        try {
            String url = USER_SERVICE_URL + "/drivers/" + driverId + "/status";
            restTemplate.patchForObject(url, "{\"status\":\"" + status + "\"}", String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update driver status: " + e.getMessage());
        }
    }
}
