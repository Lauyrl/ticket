package com.example.ticket.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private Long eventId;
    private String eventName;
    private Long holdId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant createdAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(OrderEntity order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(OrderItemResponse::from).toList()
                : List.of();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .eventId(order.getEvent().getId())
                .eventName(order.getEvent().getName())
                .holdId(order.getHold().getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
