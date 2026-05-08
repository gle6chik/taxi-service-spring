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

    private static final String TRIP_SERVICE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";

    public NotificationProcessor(NotificationTaskRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void processTask(NotificationTask task) {
        try {
            log.info("[TASK-{}] Processing: {}", task.getId(), task.getMessage());

            // Simulate notification sending (1-3 seconds)
            int sendDelay = 1000 + (int)(Math.random() * 2000);
            Thread.sleep(sendDelay);

            // Simulate ride if this is a trip start notification
            if (task.getMessage() != null && task.getMessage().contains("started")) {

                // Update trip status to STARTED
                updateTripStatus(task.getTripId(), "STARTED");

                log.info("[TASK-{}] Trip #{} started. Simulating ride for {} seconds...",
                        task.getId(), task.getTripId(), RIDE_DURATION_MS / 1000);
                Thread.sleep(RIDE_DURATION_MS);

                // Update trip status to COMPLETED
                updateTripStatus(task.getTripId(), "COMPLETED");

                // Update driver status to FREE
                updateDriverStatus(task.getRecipientId(), "FREE");

                log.info("[TASK-{}] Trip #{} completed!", task.getId(), task.getTripId());
            }

            repository.updateStatus(task.getId(), "SENT");
            log.info("[TASK-{}] Successfully sent", task.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleFailure(task);
        } catch (Exception e) {
            log.error("[TASK-{}] Failed: {}", task.getId(), e.getMessage());
            handleFailure(task);
        }
    }

    private void updateTripStatus(Long tripId, String status) {
        try {
            String url = TRIP_SERVICE_URL + "/trips/" + tripId + "/status";
            Map<String, String> body = Map.of("status", status);
            restTemplate.patchForObject(url, body, Object.class);
            log.info("[TRIP-{}] Status updated to {}", tripId, status);
        } catch (Exception e) {
            log.error("Failed to update trip {} status: {}", tripId, e.getMessage());
        }
    }

    private void updateDriverStatus(Long driverId, String status) {
        try {
            String url = USER_SERVICE_URL + "/drivers/" + driverId + "/status";
            Map<String, String> body = Map.of("status", status);
            restTemplate.put(url, body);
            log.info("[DRIVER-{}] Status updated to {}", driverId, status);
        } catch (Exception e) {
            log.error("Failed to update driver {} status: {}", driverId, e.getMessage());
        }
    }

    private void handleFailure(NotificationTask task) {
        int attempts = task.getAttemptCount() + 1;
        if (attempts < MAX_RETRIES) {
            task.setAttemptCount(attempts);
            task.setStatus("PENDING");
            repository.save(task);
            log.info("[TASK-{}] Returned to queue (attempt {}/{})",
                    task.getId(), attempts, MAX_RETRIES);
        } else {
            repository.updateStatus(task.getId(), "FAILED");
            log.error("[TASK-{}] Permanently failed after {} attempts",
                    task.getId(), MAX_RETRIES);
        }
    }
}
