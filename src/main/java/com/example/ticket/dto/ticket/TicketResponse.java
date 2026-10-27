package com.example.ticket.dto.ticket;

import java.time.Instant;

import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.order.OrderItemEntity;
import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;

public record TicketResponse(
        Long id,
        String ticketCode,
        Long eventId,
        String eventName,
        String zoneName,
        Integer rowIndex,
        Integer seatNumber,
        String status,
        Instant issuedAt,
        Instant usedAt) {

    public static TicketResponse of(TicketEntity t, SeatEntity seat, String eventName) {
        return new TicketResponse(
                t.getId(),
                t.getTicketCode() != null ? String.valueOf(t.getTicketCode()) : null,
                t.getEventId(),
                eventName,
                seat != null && seat.getZone() != null ? seat.getZone().getName() : null,
                seat != null ? seat.getRowIndex() : null,
                seat != null ? seat.getSeatNumber() : null,
                t.getStatus() != null ? t.getStatus().name() : "VALID",
                t.getIssuedAt(),
                t.getUsedAt());
    }

    public static TicketResponse from(TicketEntity t, SeatEntity seat, String eventName) {
        return of(t, seat, eventName);
    }

    public static TicketResponse from(TicketEntity t) {
        if (t == null) return null;
        return new TicketResponse(
                t.getId(),
                t.getTicketCode() != null ? String.valueOf(t.getTicketCode()) : null,
                t.getEventId(),
                null,
                null,
                null,
                null,
                t.getStatus() != null ? t.getStatus().name() : "VALID",
                t.getIssuedAt(),
                t.getUsedAt());
    }

    public static TicketResponse from(OrderItemEntity item) {
        if (item == null) return null;
        var seat = item.getSeat();
        var event = (seat != null) ? seat.getEvent() : null;
        var zone = (seat != null) ? seat.getZone() : null;

        String orderCode = (item.getOrder() != null && item.getOrder().getOrderCode() != null)
                ? item.getOrder().getOrderCode()
                : "ORD-123";
        String ticketCodeStr = orderCode + "-T" + item.getId();

        return new TicketResponse(
                item.getId(),
                ticketCodeStr,
                (event != null) ? event.getId() : null,
                (event != null) ? event.getName() : null,
                (zone != null) ? zone.getName() : null,
                (seat != null) ? seat.getRowIndex() : null,
                (seat != null) ? seat.getSeatNumber() : null,
                "VALID",
                item.getCreatedAt(),
                null);
    }
}