package com.example.ticket.dto.order;

import java.math.BigDecimal;

import com.example.ticket.entity.order.OrderItemEntity;

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
public class OrderItemResponse {

    private Long id;
    private Long seatId;
    private String zoneName;
    private String rowLabel;
    private Integer seatNumber;
    private BigDecimal unitPrice;

    public static OrderItemResponse from(OrderItemEntity item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .seatId(item.getSeat().getId())
                .zoneName(item.getZoneName())
                .rowLabel(item.getRowLabel())
                .seatNumber(item.getSeatNumber())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
