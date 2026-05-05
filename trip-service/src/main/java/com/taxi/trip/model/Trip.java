package com.taxi.trip.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Data
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(nullable = false)
    private String status; // CREATED, DRIVER_ASSIGNED, STARTED, COMPLETED, CANCELLED

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    private Double price;

    private Integer rating; // 1-5 звёзд

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
