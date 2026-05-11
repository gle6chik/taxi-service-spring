package com.taxi.trip.repository;

import com.taxi.trip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerId(Long passengerId);

    long countByPassengerId(Long passengerId);

    @Query("SELECT COALESCE(AVG(t.price), 0) FROM Trip t WHERE t.passengerId = :passengerId")
    double getAveragePriceByPassengerId(@Param("passengerId") Long passengerId);
}
