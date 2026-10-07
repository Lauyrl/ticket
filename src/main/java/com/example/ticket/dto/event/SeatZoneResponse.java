package com.example.ticket.dto.event;

import java.math.BigDecimal;
import java.util.List;

import com.example.ticket.entity.event.SeatZoneEntity;

public record SeatZoneResponse(
    Long id,
    // Long eventId,   --- seat already accessed via @GetMapping("/api/v1/events/{eventId}/seats")
    String name,
    BigDecimal price,
    List<SeatResponse> seats
) {
    public SeatZoneResponse(SeatZoneEntity e, List<SeatResponse> seats) {
        this(
            e.getId(), 
            e.getName(), 
            e.getPrice(), 
            seats
        );
    }
}
