package com.postgrespro.flights.dto;

import java.math.BigDecimal;
import java.util.List;

public class BookingDto {
    private String passengerId;
    private String passengerName;
    private List<Integer> flightIds;
    private String fareConditions;
    private BigDecimal totalPrice;

    // Getters and setters
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public List<Integer> getFlightIds() { return flightIds; }
    public void setFlightIds(List<Integer> flightIds) { this.flightIds = flightIds; }
    public String getFareConditions() { return fareConditions; }
    public void setFareConditions(String fareConditions) { this.fareConditions = fareConditions; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
