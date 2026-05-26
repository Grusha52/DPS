package com.postgrespro.flights.dto;

import java.time.LocalTime;
import java.util.List;

public class ScheduleDto {
    private List<Integer> daysOfWeek;
    private LocalTime time;
    private String flightNo;
    private String location; // Origin or Destination

    public ScheduleDto(List<Integer> daysOfWeek, LocalTime time, String flightNo, String location) {
        this.daysOfWeek = daysOfWeek;
        this.time = time;
        this.flightNo = flightNo;
        this.location = location;
    }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getFlightNo() { return flightNo; }
    public void setFlightNo(String flightNo) { this.flightNo = flightNo; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
