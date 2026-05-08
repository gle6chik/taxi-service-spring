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
    public Trip createTrip(Long passengerId, String origin, String destination, Double distance, String tariffType) {
        // Checking passenger existing
        checkPassengerExists(passengerId);

        // Atomically find and assign driver
        Long driverId = findAndAssignDriver();
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
        trip.setPrice(calculatePrice(distance, tariffType));
        trip.setDistance(distance);
        trip.setTariffType(tariffType);

        Trip savedTrip = tripRepository.save(trip);

        // Send task in queue
        sendNotification(savedTrip.getId(), driverId, "DRIVER", "Trip #" + savedTrip.getId() + " started");

        return savedTrip;
    }

    private void checkPassengerExists(Long passengerId) {
        try {
            restTemplate.getForObject(USER_SERVICE_URL + "/passengers/" + passengerId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Passenger not found: " + passengerId);
        }
    }

    private Long findAndAssignDriver() {
        try {
            String url = USER_SERVICE_URL + "/drivers/assign";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            if (response != null) {
                return Long.valueOf(response.get("id").toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign driver: " + e.getMessage());
        }
        return null;
    }

    private Double calculatePrice(Double distance, String tariffType) {
        // Price per kilometer
        double ECONOMY_RATE = 50.0;
        double COMFORT_RATE = 100.0;
        double BUSINESS_RATE = 200.0;

        double rate = switch (tariffType != null ? tariffType : "ECONOMY") {
            case "COMFORT" -> COMFORT_RATE;
            case "BUSINESS" -> BUSINESS_RATE;
            default -> ECONOMY_RATE;
        };

        double price = distance * rate;
        return Math.round(price * 100.0) / 100.0;
    }

    private void sendNotification(Long tripId, Long recipientId,
                                  String recipientType, String message) {
        try {
            String url = "http://localhost:8083/notifications";
            Map<String, Object> body = Map.of(
                    "tripId", tripId,
                    "recipientId", recipientId,
                    "recipientType", recipientType,
                    "message", message
            );
            restTemplate.postForObject(url, body, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to queue notification: " + e.getMessage());
        }
    }
}
