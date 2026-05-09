package com.taxi.trip.service;

import com.taxi.trip.TripServiceApplication;
import com.taxi.trip.model.Trip;
import com.taxi.trip.repository.TripRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service:8081";
    private static final String NOTIFICATION_SERVICE_URL = "http://notification-service:8083";

    public TripService(TripRepository tripRepository, RestTemplate restTemplate) {
        this.tripRepository = tripRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Trip createTrip(Long passengerId, String origin, String destination, Double distance, String tariffType) {
        // Checking passenger existing
        checkPassengerExists(passengerId);

        // Create trip
        Trip trip = new Trip();
        trip.setPassengerId(passengerId);
        // Driver not found while
        trip.setOrigin(origin);
        trip.setDestination(destination);
        trip.setStatus("CREATED");
        trip.setPrice(calculatePrice(distance, tariffType));
        trip.setDistance(distance);
        trip.setTariffType(tariffType);

        Trip savedTrip = tripRepository.save(trip);

        sendNotification(savedTrip.getId(), passengerId, "PASSENGER","Trip #" + savedTrip.getId() + " created.");
        sendNotification(savedTrip.getId(), passengerId, "PASSENGER", "Searching for a driver...");

        return savedTrip;
    }

    private void checkPassengerExists(Long passengerId) {
        try {
            restTemplate.getForObject(USER_SERVICE_URL + "/passengers/" + passengerId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Passenger not found: " + passengerId);
        }
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
            String url = NOTIFICATION_SERVICE_URL + "/notifications";
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
