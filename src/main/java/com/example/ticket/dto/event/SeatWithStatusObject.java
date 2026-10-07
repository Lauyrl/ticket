package com.example.ticket.dto.event;

public interface SeatWithStatusObject {
    Long getId();
    Long getZoneId();
    Integer getRowIndex();
    Integer getSeatNumber();
    String getStatus();
}
