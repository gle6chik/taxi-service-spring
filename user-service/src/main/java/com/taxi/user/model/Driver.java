package com.taxi.user.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Data
public class Driver implements java.io.Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(name = "license_number")
    private String licenseNumber;

    private String status;  // FREE, BUSY, OFFLINE

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
