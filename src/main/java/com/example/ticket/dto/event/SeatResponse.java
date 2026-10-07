package com.example.ticket.dto.event;

import com.example.ticket.enums.SeatStatus;

public record SeatResponse(
    Long id, 
    // Long eventId,   --- seat already accessed via @GetMapping("/api/v1/events/{eventId}/seats")
    int rowIndex,
    int seatNumber, // cycles per row
    SeatStatus status
) {
    public SeatResponse(SeatWithStatusObject e) {
        this(
            e.getId(), 
            e.getRowIndex(), 
            e.getSeatNumber(),
            SeatStatus.valueOf(e.getStatus())
        );
    }
}
