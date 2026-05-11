package com.taxi.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_tasks", indexes = {
        @Index(name = "idx_notification_status", columnList = "status")
})
@Data
public class NotificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType; // PASSENGER, DRIVER

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, SENT, FAILED

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
