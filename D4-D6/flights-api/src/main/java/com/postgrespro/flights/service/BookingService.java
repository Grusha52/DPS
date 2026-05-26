package com.postgrespro.flights.service;

import com.postgrespro.flights.dto.BookingDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String createBooking(BookingDto request) {
        String bookRef = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        // Insert booking
        String insertBooking = "INSERT INTO bookings.bookings(book_ref, book_date, total_amount) " +
                                  "VALUES (:bookRef, bookings.now(), :totalAmount)";
                                  
        jdbcTemplate.update(insertBooking, new MapSqlParameterSource()
            .addValue("bookRef", bookRef)
            .addValue("totalAmount", request.getTotalPrice()));
            
        // Insert ticket
        String ticketNo = "00054" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
        String insertTicket = "INSERT INTO bookings.tickets(ticket_no, book_ref, passenger_id, passenger_name, outbound) " +
                                 "VALUES (:ticketNo, :bookRef, :passengerId, :passengerName, TRUE)";
        
        jdbcTemplate.update(insertTicket, new MapSqlParameterSource()
            .addValue("ticketNo", ticketNo)
            .addValue("bookRef", bookRef)
            .addValue("passengerId", request.getPassengerId())
            .addValue("passengerName", request.getPassengerName()));
            
        // Insert segments
        String insertSegment = "INSERT INTO bookings.segments(ticket_no, flight_id, fare_conditions, price) " +
                                  "VALUES (:ticketNo, :flightId, :fareConditions, " +
                                  "(SELECT price FROM public.pricing_rules pr JOIN bookings.flights f ON pr.route_no = f.route_no WHERE f.flight_id = :flightId AND pr.fare_conditions = :fareConditions LIMIT 1))";
                                  
        for (Integer flightId : request.getFlightIds()) {
            jdbcTemplate.update(insertSegment, new MapSqlParameterSource()
                .addValue("ticketNo", ticketNo)
                .addValue("flightId", flightId)
                .addValue("fareConditions", request.getFareConditions()));
        }

        return bookRef;
    }

    @Transactional
    public String checkIn(String ticketNo, Integer flightId) {
        // Find an available seat
        // Seats that are valid for the airplane of this flight and fare conditions of the segment
        // AND not already taken by someone else (not in boarding_passes for this flight)
        String findSeatSql = """
            SELECT s.seat_no 
            FROM bookings.seats s
            JOIN bookings.flights f ON f.flight_id = :flightId
            JOIN bookings.routes r ON r.route_no = f.route_no AND r.validity @> f.scheduled_departure
            JOIN bookings.segments seg ON seg.flight_id = f.flight_id AND seg.ticket_no = :ticketNo
            WHERE s.airplane_code = r.airplane_code 
              AND s.fare_conditions = seg.fare_conditions
              AND s.seat_no NOT IN (
                  SELECT bp.seat_no FROM bookings.boarding_passes bp WHERE bp.flight_id = :flightId
              )
            LIMIT 1
            """;
            
        List<String> seats = jdbcTemplate.query(findSeatSql, new MapSqlParameterSource()
            .addValue("flightId", flightId)
            .addValue("ticketNo", ticketNo),
            (rs, rowNum) -> rs.getString("seat_no"));
            
        if (seats.isEmpty()) {
            throw new RuntimeException("No available seats or invalid ticket/flight");
        }
        String seatNo = seats.get(0);
        
        // Get next boarding_no
        String nextBoardingNoSql = "SELECT COALESCE(MAX(boarding_no), 0) + 1 FROM bookings.boarding_passes WHERE flight_id = :flightId";
        Integer nextBoardingNo = jdbcTemplate.queryForObject(nextBoardingNoSql, new MapSqlParameterSource("flightId", flightId), Integer.class);
        
        // Insert boarding pass
        String insertBp = """
            INSERT INTO bookings.boarding_passes(ticket_no, flight_id, seat_no, boarding_no, boarding_time)
            VALUES (:ticketNo, :flightId, :seatNo, :boardingNo, bookings.now())
            """;
        jdbcTemplate.update(insertBp, new MapSqlParameterSource()
            .addValue("ticketNo", ticketNo)
            .addValue("flightId", flightId)
            .addValue("seatNo", seatNo)
            .addValue("boardingNo", nextBoardingNo));
            
        return seatNo;
    }
}
