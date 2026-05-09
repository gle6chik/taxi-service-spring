package com.taxi.notification.worker;

import com.taxi.notification.model.NotificationTask;
import com.taxi.notification.repository.NotificationTaskRepository;
import com.taxi.notification.service.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);
    private static final long POLL_DELAY_MS = 1000;

    private final NotificationTaskRepository repository;
    private final NotificationProcessor processor;
    private final AtomicBoolean running;
    private final String workerName;
    private final TransactionTemplate transactionTemplate;

    public NotificationWorker(String workerName,
                              NotificationTaskRepository repository,
                              NotificationProcessor processor,
                              AtomicBoolean running,
                              TransactionTemplate transactionTemplate) {
        this.workerName = workerName;
        this.repository = repository;
        this.processor = processor;
        this.running = running;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run() {
        log.info("{} started", workerName);

        while (running.get()) {
            try {
                transactionTemplate.execute(status -> {
                    Optional<NotificationTask> taskOpt = repository.findNextPendingTask();

                    if (taskOpt.isPresent()) {
                        NotificationTask task = taskOpt.get();
                        repository.updateStatus(task.getId(), "PROCESSING");
                        processor.processTask(task);
                    }
                    return null;
                });

                Thread.sleep(POLL_DELAY_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("{} interrupted", workerName);
                break;
            } catch (Exception e) {
                log.error("{} error: {}", workerName, e.getMessage());
            }
        }

        log.info("{} stopped", workerName);
    }
}
