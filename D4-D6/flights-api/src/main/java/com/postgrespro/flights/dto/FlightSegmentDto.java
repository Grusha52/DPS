package com.postgrespro.flights.dto;

import java.time.LocalDateTime;

public class FlightSegmentDto {
    private String flightNo;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime scheduledArrival;

    public FlightSegmentDto(String flightNo, String departureAirport, String arrivalAirport, LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival) {
        this.flightNo = flightNo;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
    }

    public String getFlightNo() { return flightNo; }
    public void setFlightNo(String flightNo) { this.flightNo = flightNo; }
    public String getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }
    public String getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }
    public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(LocalDateTime scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }
    public LocalDateTime getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(LocalDateTime scheduledArrival) { this.scheduledArrival = scheduledArrival; }
}
