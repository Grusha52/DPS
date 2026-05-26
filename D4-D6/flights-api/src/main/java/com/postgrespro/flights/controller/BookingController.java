package com.postgrespro.flights.controller;

import com.postgrespro.flights.dto.BookingDto;
import com.postgrespro.flights.service.BookingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public Map<String, String> createBooking(@RequestBody BookingDto request) {
        String bookRef = bookingService.createBooking(request);
        return Map.of("book_ref", bookRef);
    }

    @PostMapping("/checkin")
    public Map<String, String> checkIn(
            @RequestParam String ticketNo, 
            @RequestParam Integer flightId) {
        String seatNo = bookingService.checkIn(ticketNo, flightId);
        return Map.of("seat_no", seatNo);
    }
}
