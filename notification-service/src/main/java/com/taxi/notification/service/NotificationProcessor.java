package com.taxi.notification.service;

import com.taxi.notification.model.NotificationTask;
import com.taxi.notification.repository.NotificationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);
    private static final int MAX_RETRIES = 3;
    private static final int RIDE_DURATION_MS = 15_000;

    private final NotificationTaskRepository repository;

    public NotificationProcessor(NotificationTaskRepository repository) {
        this.repository = repository;
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
                log.info("[TASK-{}] Trip #{} started. Simulating ride for {} seconds...",
                        task.getId(), task.getTripId(), RIDE_DURATION_MS / 1000);
                Thread.sleep(RIDE_DURATION_MS);
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

    private void simulateWork(int minMs, int maxMs) throws InterruptedException {
        long delay = minMs + (long)(Math.random() * (maxMs - minMs));
        Thread.sleep(delay);
    }
}
