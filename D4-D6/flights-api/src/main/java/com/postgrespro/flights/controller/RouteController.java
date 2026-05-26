package com.postgrespro.flights.controller;

import com.postgrespro.flights.dto.RouteDto;
import com.postgrespro.flights.service.RouteSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin("*")
public class RouteController {

    private final RouteSearchService routeSearchService;

    public RouteController(RouteSearchService routeSearchService) {
        this.routeSearchService = routeSearchService;
    }

    @GetMapping
    public List<RouteDto> searchRoutes(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam String bookingClass,
            @RequestParam(required = false, defaultValue = "3") Integer maxConnections) {
        
        return routeSearchService.searchRoutes(origin, destination, departureDate, bookingClass, maxConnections);
    }
}
