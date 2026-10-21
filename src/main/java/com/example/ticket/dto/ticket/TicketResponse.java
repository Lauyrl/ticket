package com.example.ticket.dto.ticket;

import java.time.Instant;
import java.util.UUID;

import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;

public record TicketResponse(
        Long id,
        UUID ticketCode,
        Long eventId,
        String eventName,
        String zoneName,
        Integer rowIndex,
        Integer seatNumber,
        TicketStatus status,
        Instant issuedAt,
        Instant usedAt) {

    public static TicketResponse of(TicketEntity t, SeatEntity seat, String eventName) {
        return new TicketResponse(
                t.getId(),
                t.getTicketCode(),
                t.getEventId(),
                eventName,
                seat.getZone().getName(),
                seat.getRowIndex(),
                seat.getSeatNumber(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getUsedAt());
    }
}