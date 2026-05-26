package com.postgrespro.flights.controller;

import com.postgrespro.flights.domain.Airport;
import com.postgrespro.flights.dto.ScheduleDto;
import com.postgrespro.flights.service.FlightService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/cities")
    public List<String> getCities() {
        return flightService.getAllCities();
    }

    @GetMapping("/airports")
    public List<Airport> getAirports() {
        return flightService.getAllAirports();
    }

    @GetMapping("/cities/{city}/airports")
    public List<Airport> getAirportsByCity(@PathVariable String city) {
        return flightService.getAirportsByCity(city);
    }

    @GetMapping("/airports/{airportCode}/inbound")
    public List<ScheduleDto> getInboundSchedule(@PathVariable String airportCode) {
        return flightService.getInboundSchedule(airportCode);
    }

    @GetMapping("/airports/{airportCode}/outbound")
    public List<ScheduleDto> getOutboundSchedule(@PathVariable String airportCode) {
        return flightService.getOutboundSchedule(airportCode);
    }
}
