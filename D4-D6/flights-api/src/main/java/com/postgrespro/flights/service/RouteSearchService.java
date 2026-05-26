package com.postgrespro.flights.service;

import com.postgrespro.flights.dto.FlightSegmentDto;
import com.postgrespro.flights.dto.RouteDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RouteSearchService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RouteSearchService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RouteDto> searchRoutes(String origin, String destination, LocalDate departureDate, 
                                       String bookingClass, Integer maxConnections) {
        if (maxConnections == null || maxConnections < 0) {
            maxConnections = 3;
        }

        String sql = """
            WITH RECURSIVE flight_search AS (
                SELECT 
                    f.flight_id,
                    f.scheduled_departure,
                    f.scheduled_arrival,
                    r.arrival_airport,
                    0 AS connections,
                    ARRAY[f.flight_id] as path,
                    p.price::numeric as total_price
                FROM bookings.flights f
                JOIN bookings.routes r ON f.route_no = r.route_no AND r.validity @> f.scheduled_departure 
                JOIN bookings.airports dep ON r.departure_airport = dep.airport_code
                JOIN public.pricing_rules p ON f.route_no = p.route_no 
                WHERE (dep.airport_code = :origin OR dep.city = :origin)
                  AND f.scheduled_departure >= :startDate::timestamp
                  AND f.scheduled_departure < (:startDate::timestamp + interval '1 day')
                  AND f.status IN ('Scheduled', 'On Time', 'Delayed')
                  AND p.fare_conditions = :bookingClass
                  
                UNION ALL
                
                SELECT 
                    f.flight_id,
                    f.scheduled_departure,
                    f.scheduled_arrival,
                    r.arrival_airport,
                    fs.connections + 1,
                    fs.path || f.flight_id,
                    fs.total_price + p.price
                FROM flight_search fs
                JOIN bookings.flights f ON f.scheduled_departure > fs.scheduled_arrival
                              AND f.scheduled_departure < fs.scheduled_arrival + interval '24 hours'
                JOIN bookings.routes r ON f.route_no = r.route_no AND r.validity @> f.scheduled_departure
                JOIN public.pricing_rules p ON f.route_no = p.route_no
                WHERE r.departure_airport = fs.arrival_airport
                  AND fs.connections < :maxConnections
                  AND NOT f.flight_id = ANY(fs.path)
                  AND f.status IN ('Scheduled', 'On Time', 'Delayed')
                  AND p.fare_conditions = :bookingClass
            )
            SELECT fs.path, fs.connections, fs.total_price 
            FROM flight_search fs
            JOIN bookings.airports arr ON fs.arrival_airport = arr.airport_code
            WHERE (arr.airport_code = :destination OR arr.city = :destination)
            ORDER BY fs.total_price ASC, fs.connections ASC
            LIMIT 50;
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("origin", origin)
            .addValue("destination", destination)
            .addValue("startDate", java.sql.Date.valueOf(departureDate))
            .addValue("bookingClass", bookingClass)
            .addValue("maxConnections", maxConnections);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        
        List<RouteDto> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Array pathArray = (Array) row.get("path");
            Integer[] fIds = null;
            try {
                fIds = (Integer[]) pathArray.getArray();
            } catch (Exception e) {
                continue;
            }
            int connections = ((Number) row.get("connections")).intValue();
            BigDecimal totalPrice = (BigDecimal) row.get("total_price");
            
            List<FlightSegmentDto> segments = fetchFlightSegments(fIds);
            results.add(new RouteDto(segments, connections, totalPrice));
        }

        return results;
    }

    private List<FlightSegmentDto> fetchFlightSegments(Integer[] flightIds) {
        if (flightIds == null || flightIds.length == 0) return new ArrayList<>();
        
        String sql = """
            SELECT f.flight_id, f.route_no, r.departure_airport, r.arrival_airport, 
                   f.scheduled_departure, f.scheduled_arrival
            FROM bookings.flights f
            JOIN bookings.routes r ON f.route_no = r.route_no AND r.validity @> f.scheduled_departure
            WHERE f.flight_id IN (:ids)
            """;
            
        MapSqlParameterSource params = new MapSqlParameterSource("ids", Arrays.asList(flightIds));
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        
        List<FlightSegmentDto> ordered = new ArrayList<>();
        for (Integer id : flightIds) {
            for (Map<String, Object> row : rows) {
                if (((Number) row.get("flight_id")).intValue() == id) {
                    ordered.add(new FlightSegmentDto(
                        (String) row.get("route_no"),
                        (String) row.get("departure_airport"),
                        (String) row.get("arrival_airport"),
                        ((Timestamp) row.get("scheduled_departure")).toLocalDateTime(),
                        ((Timestamp) row.get("scheduled_arrival")).toLocalDateTime()
                    ));
                    break;
                }
            }
        }
        return ordered;
    }
}
