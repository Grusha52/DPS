package com.postgrespro.flights.repository;

import com.postgrespro.flights.domain.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirportRepository extends JpaRepository<Airport, String> {

    @Query("SELECT DISTINCT a.city FROM Airport a ORDER BY a.city")
    List<String> findAllCities();

    List<Airport> findAllByOrderByAirportNameAsc();

    List<Airport> findByCityOrderByAirportNameAsc(String city);
}
