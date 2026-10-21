package com.example.ticket.dto.ticket;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ticket.entity.order.OrderItemEntity;

public record TicketResponse(
    Long id,
    String ticketCode,
    Long orderId,
    String orderCode,
    Long eventId,
    String eventName,
    String venue,
    String city,
    Instant eventStartsAt,
    String zoneName,
    String rowLabel,
    Integer seatNumber,
    BigDecimal unitPrice,
    String status,
    Instant purchasedAt
) {
    public static TicketResponse from(OrderItemEntity item) {
        String ticketCode = item.getOrder().getOrderCode() + "-T" + item.getId();
        String status = item.getOrder().getStatus().name().equals("PAID") ? "VALID" : item.getOrder().getStatus().name();
        Instant purchasedAt = item.getOrder().getPaidAt() != null ? item.getOrder().getPaidAt() : item.getCreatedAt();

        return new TicketResponse(
            item.getId(),
            ticketCode,
            item.getOrder().getId(),
            item.getOrder().getOrderCode(),
            item.getOrder().getEvent().getId(),
            item.getOrder().getEvent().getName(),
            item.getOrder().getEvent().getVenue(),
            item.getOrder().getEvent().getCity(),
            item.getOrder().getEvent().getStartsAt(),
            item.getZoneName(),
            item.getRowLabel(),
            item.getSeatNumber(),
            item.getUnitPrice(),
            status,
            purchasedAt
        );
    }
}
