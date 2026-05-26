package com.postgrespro.flights.dto;

import java.math.BigDecimal;
import java.util.List;

public class RouteDto {
    private List<FlightSegmentDto> flights;
    private int connections;
    private BigDecimal totalPrice;

    public RouteDto(List<FlightSegmentDto> flights, int connections, BigDecimal totalPrice) {
        this.flights = flights;
        this.connections = connections;
        this.totalPrice = totalPrice;
    }

    public List<FlightSegmentDto> getFlights() { return flights; }
    public void setFlights(List<FlightSegmentDto> flights) { this.flights = flights; }
    public int getConnections() { return connections; }
    public void setConnections(int connections) { this.connections = connections; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
