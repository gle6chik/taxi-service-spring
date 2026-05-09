package com.taxi.notification.worker;

import com.taxi.notification.repository.NotificationTaskRepository;
import com.taxi.notification.service.NotificationProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerPool {
    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);
    private static final int WORKER_COUNT = 4;
    private static final int SHUTDOWN_TIMEOUT_SEC = 30;

    private final NotificationTaskRepository repository;
    private final NotificationProcessor processor;
    private final TransactionTemplate transactionTemplate;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public WorkerPool(NotificationTaskRepository repository,
                      NotificationProcessor processor, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.processor = processor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void start() {
        executorService = Executors.newFixedThreadPool(WORKER_COUNT);

        for (int i = 0; i < WORKER_COUNT; i++) {
            String workerName = "Worker-" + (i + 1);
            Runnable worker = new NotificationWorker(workerName, repository, processor, running, transactionTemplate);
            executorService.submit(worker);
        }

        log.info("Worker pool started with {} threads", WORKER_COUNT);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down worker pool...");
        running.set(false);
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                log.warn("Worker pool forced shutdown after {}s timeout", SHUTDOWN_TIMEOUT_SEC);
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Worker pool stopped");
    }
}
