package com.taxi.user.controller;

import com.taxi.user.model.Driver;
import com.taxi.user.model.Passenger;
import com.taxi.user.repository.DriverRepository;
import com.taxi.user.repository.PassengerRepository;
import com.taxi.user.security.JwtService;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(PassengerRepository passengerRepository,
                          DriverRepository driverRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register/passenger")
    public Map<String, String> registerPassenger(@RequestBody RegisterRequest request) {
        Passenger passenger = new Passenger();
        passenger.setName(request.getName());
        passenger.setEmail(request.getEmail());
        passenger.setPhone(request.getPhone());
        passenger.setPassword(passwordEncoder.encode(request.getPassword()));
        passengerRepository.save(passenger);

        String token = jwtService.generateToken(request.getEmail(), "PASSENGER");
        return Map.of("token", token);
    }

    @PostMapping("/register/driver")
    public Map<String, String> registerDriver(@RequestBody RegisterDriverRequest request) {
        Driver driver = new Driver();
        driver.setName(request.getName());
        driver.setEmail(request.getEmail());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setPassword(passwordEncoder.encode(request.getPassword()));
        driver.setStatus("FREE");
        driverRepository.save(driver);

        String token = jwtService.generateToken(request.getEmail(), "DRIVER");
        return Map.of("token", token);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        // Ищем среди пассажиров
        Passenger passenger = passengerRepository.findAll().stream()
                .filter(p -> p.getEmail().equals(request.getEmail()))
                .findFirst().orElse(null);

        if (passenger != null && passwordEncoder.matches(request.getPassword(), passenger.getPassword())) {
            String token = jwtService.generateToken(passenger.getEmail(), "PASSENGER");
            return Map.of("token", token);
        }

        // Ищем среди водителей
        Driver driver = driverRepository.findAll().stream()
                .filter(d -> d.getEmail().equals(request.getEmail()))
                .findFirst().orElse(null);

        if (driver != null && passwordEncoder.matches(request.getPassword(), driver.getPassword())) {
            String token = jwtService.generateToken(driver.getEmail(), "DRIVER");
            return Map.of("token", token);
        }

        throw new RuntimeException("Invalid credentials");
    }

    @Data
    static class RegisterRequest {
        private String name;
        private String email;
        private String phone;
        private String password;
    }

    @Data
    static class RegisterDriverRequest {
        private String name;
        private String email;
        private String phone;
        private String licenseNumber;
        private String password;
    }

    @Data
    static class LoginRequest {
        private String email;
        private String password;
    }
}
