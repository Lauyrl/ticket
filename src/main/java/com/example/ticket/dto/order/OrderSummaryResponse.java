package com.example.ticket.dto.order;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ticket.entity.order.OrderEntity;
import com.example.ticket.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponse {

    private Long id;
    private String orderCode;
    private Long eventId;
    private String eventName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private int totalItems;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant createdAt;

    public static OrderSummaryResponse from(OrderEntity order) {
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .eventId(order.getEvent().getId())
                .eventName(order.getEvent().getName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalItems(order.getItems() != null ? order.getItems().size() : 0)
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
