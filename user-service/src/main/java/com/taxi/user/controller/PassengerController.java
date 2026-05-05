package com.taxi.user.controller;

import com.taxi.user.model.Passenger;
import com.taxi.user.repository.PassengerRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/passengers")
public class PassengerController {

    private final PassengerRepository repository;

    public PassengerController(PassengerRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Passenger create(@RequestBody Passenger passenger) {
        return repository.save(passenger);
    }

    @GetMapping("/{id}")
    public Passenger getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
    }
}