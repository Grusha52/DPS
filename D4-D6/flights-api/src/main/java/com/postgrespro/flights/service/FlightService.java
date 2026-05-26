package com.postgrespro.flights.service;

import com.postgrespro.flights.domain.Airport;
import com.postgrespro.flights.dto.ScheduleDto;
import com.postgrespro.flights.repository.AirportRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Time;
import java.util.Arrays;
import java.util.List;

@Service
public class FlightService {

    private final AirportRepository airportRepository;
    private final JdbcTemplate jdbcTemplate;

    public FlightService(AirportRepository airportRepository, JdbcTemplate jdbcTemplate) {
        this.airportRepository = airportRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getAllCities() {
        return airportRepository.findAllCities();
    }

    public List<Airport> getAllAirports() {
        return airportRepository.findAllByOrderByAirportNameAsc();
    }

    public List<Airport> getAirportsByCity(String city) {
        return airportRepository.findByCityOrderByAirportNameAsc(city);
    }

    public List<ScheduleDto> getInboundSchedule(String airportCode) {
        String query = "SELECT days_of_week, scheduled_time + duration as time_of_arrival, route_no, departure_airport " +
                       "FROM bookings.routes WHERE arrival_airport = ?";
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Array daysArray = rs.getArray("days_of_week");
            Integer[] days = (Integer[]) daysArray.getArray();
            Time scheduledTime = rs.getTime("time_of_arrival");
            
            return new ScheduleDto(Arrays.asList(days), scheduledTime.toLocalTime(), rs.getString("route_no"), rs.getString("departure_airport"));
        }, airportCode);
    }
    
    public List<ScheduleDto> getOutboundSchedule(String airportCode) {
        String query = "SELECT days_of_week, scheduled_time as time_of_departure, route_no, arrival_airport " +
                       "FROM bookings.routes WHERE departure_airport = ?";
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Array daysArray = rs.getArray("days_of_week");
            Integer[] days = (Integer[]) daysArray.getArray();
            Time scheduledTime = rs.getTime("time_of_departure");
            
            return new ScheduleDto(Arrays.asList(days), scheduledTime.toLocalTime(), rs.getString("route_no"), rs.getString("arrival_airport"));
        }, airportCode);
    }
}
