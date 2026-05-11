package com.taxi.notification.service;

import com.taxi.notification.model.NotificationTask;
import com.taxi.notification.repository.NotificationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);
    private static final int MAX_RETRIES = 3;
    private static final int RIDE_DURATION_MS = 15_000;

    private final NotificationTaskRepository repository;
    private final RestTemplate restTemplate;

    private static final String TRIP_SERVICE_URL = "http://trip-service:8082";
    private static final String USER_SERVICE_URL = "http://user-service:8081";
    private static final String NOTIFICATION_SERVICE_URL = "http://notification-service:8083";

    public NotificationProcessor(NotificationTaskRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void processTask(NotificationTask task) {
        try {
            log.info("[TASK-{}] Processing: {}", task.getId(), task.getMessage());

            if (task.getMessage() != null && task.getMessage().contains("Searching for a driver")) {
                findAndAssignDriver(task);
                return;
            }

            // Thread.sleep(1000 + (int)(Math.random() * 2000));

            if (task.getMessage() != null
                    && task.getMessage().contains("started")
                    && "DRIVER".equals(task.getRecipientType())) {

                Long passengerId = getPassengerId(task.getTripId());

                updateTripStatus(task.getTripId(), "STARTED");

                createNotification(task.getTripId(), passengerId, "PASSENGER",
                        "Trip #" + task.getTripId() + " started!");

                Thread.sleep(RIDE_DURATION_MS);

                updateTripStatus(task.getTripId(), "COMPLETED");
                updateDriverStatus(task.getRecipientId(), "FREE");

                createNotification(task.getTripId(), passengerId, "PASSENGER",
                        "Trip #" + task.getTripId() + " completed.");
                createNotification(task.getTripId(), task.getRecipientId(), "DRIVER",
                        "Trip #" + task.getTripId() + " completed.");
            }

            repository.updateStatus(task.getId(), "SENT");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleFailure(task);
        } catch (Exception e) {
            log.error("[TASK-{}] Failed: {}", task.getId(), e.getMessage());
            handleFailure(task);
        }
    }

    private void findAndAssignDriver(NotificationTask task) {
        int attempt = task.getAttemptCount() + 1;

        createNotification(task.getTripId(), task.getRecipientId(), "PASSENGER",
                "Searching driver... Attempt " + attempt + " of " + MAX_RETRIES);

        try {
            String url = USER_SERVICE_URL + "/drivers/assign";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

            if (response != null) {
                Long driverId = Long.valueOf(response.get("id").toString());

                updateTripDriver(task.getTripId(), driverId);
                updateTripStatus(task.getTripId(), "DRIVER_ASSIGNED");

                createNotification(task.getTripId(), task.getRecipientId(), "PASSENGER",
                        "Driver found! Trip #" + task.getTripId() + " starts soon.");

                createNotification(task.getTripId(), driverId, "DRIVER",
                        "Trip #" + task.getTripId() + " started");

                repository.updateStatus(task.getId(), "SENT");
                return;
            }
        } catch (Exception e) {
            log.warn("[TASK-{}] Error searching driver: {}", task.getId(), e.getMessage());
        }

        if (attempt >= MAX_RETRIES) {
            createNotification(task.getTripId(), task.getRecipientId(), "PASSENGER",
                    "No drivers available. Trip #" + task.getTripId() + " cancelled.");
            updateTripStatus(task.getTripId(), "CANCELLED");
            repository.updateStatus(task.getId(), "SENT");
        } else {
            task.setAttemptCount(attempt);
            task.setStatus("PENDING");
            repository.save(task);
        }
    }

    private void updateTripDriver(Long tripId, Long driverId) {
        try {
            String url = TRIP_SERVICE_URL + "/trips/" + tripId + "/driver";
            Map<String, Long> body = Map.of("driverId", driverId);
            restTemplate.patchForObject(url, body, Object.class);
        } catch (Exception e) {
            log.error("Failed to update trip driver: {}", e.getMessage());
        }
    }

    private Long getPassengerId(Long tripId) {
        try {
            String url = TRIP_SERVICE_URL + "/trips/" + tripId;
            Map<String, Object> trip = restTemplate.getForObject(url, Map.class);
            return trip != null ? Long.valueOf(trip.get("passengerId").toString()) : null;
        } catch (Exception e) {
            log.error("Failed to get trip info: {}", e.getMessage());
            return null;
        }
    }

    private void createNotification(Long tripId, Long recipientId,
                                    String recipientType, String message) {
        try {
            Map<String, Object> body = Map.of(
                    "tripId", tripId,
                    "recipientId", recipientId,
                    "recipientType", recipientType,
                    "message", message
            );
            restTemplate.postForObject(NOTIFICATION_SERVICE_URL + "/notifications", body, Object.class);
        } catch (Exception e) {
            log.error("Failed to create notification: {}", e.getMessage());
        }
    }

    private void updateTripStatus(Long tripId, String status) {
        try {
            String url = TRIP_SERVICE_URL + "/trips/" + tripId + "/status";
            Map<String, String> body = Map.of("status", status);
            restTemplate.patchForObject(url, body, Object.class);
        } catch (Exception e) {
            log.error("Failed to update trip status: {}", e.getMessage());
        }
    }

    private void updateDriverStatus(Long driverId, String status) {
        try {
            String url = USER_SERVICE_URL + "/drivers/" + driverId + "/status";
            Map<String, String> body = Map.of("status", status);
            restTemplate.patchForObject(url, body, Object.class);
        } catch (Exception e) {
            log.error("Failed to update driver status: {}", e.getMessage());
        }
    }

    private void handleFailure(NotificationTask task) {
        int attempts = task.getAttemptCount();
        if (attempts < MAX_RETRIES) {
            task.setStatus("PENDING");
            repository.save(task);
        } else {
            repository.updateStatus(task.getId(), "FAILED");
        }
    }
}