package com.example.ticket.dto.event;

import java.time.Instant;

import com.example.ticket.entity.event.SeatHoldEntity;
import com.example.ticket.enums.SeatHoldStatus;

public record SeatHoldResponse(
    Long id,
    Long seatId,
    SeatHoldStatus status,
    Instant expiresAt
) {
    public SeatHoldResponse(SeatHoldEntity e) {
        this(
            e.getId(),
            e.getSeat().getId(),
            e.getStatus(),
            e.getExpiresAt()
        );
    }
}
