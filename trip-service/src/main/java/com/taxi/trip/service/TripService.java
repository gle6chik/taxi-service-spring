package com.taxi.trip.service;

import com.taxi.trip.model.Trip;
import com.taxi.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final RestTemplate restTemplate;

    private final RestClient.Builder restClientBuilder;

    private static final String USER_SERVICE_URL = "http://localhost:8081";

    public TripService(TripRepository tripRepository, RestTemplate restTemplate,
                       RestClient.Builder restClientBuilder) {
        this.tripRepository = tripRepository;
        this.restTemplate = restTemplate;
        this.restClientBuilder = restClientBuilder;
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
        try {
            List<Map> drivers = restTemplate.getForObject(USER_SERVICE_URL + "/drivers/available", List.class);
            if (drivers != null && !drivers.isEmpty()) {
                Map<String, Object> driver = drivers.get(0);
                return Long.valueOf(driver.get("id").toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find available driver: " + e.getMessage());
        }
        return null;
    }

    private Double calculatePrice(String origin, String destination) {
        // TODO: calculate trip price (distance * tariff)
        return 1000D; // Mock
    }

    private void updateDriverStatus(Long driverId, String status) {
        try {
            String url = USER_SERVICE_URL + "/drivers/" + driverId + "/status";

            // Создаём тело запроса
            Map<String, String> body = Map.of("status", status);

            // Создаём HTTP-entity с заголовками
            org.springframework.http.HttpEntity<Map<String, String>> requestEntity =
                    new org.springframework.http.HttpEntity<>(body);

            // Используем exchange с методом PATCH
            restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update driver status: " + e.getMessage());
        }
    }
}
