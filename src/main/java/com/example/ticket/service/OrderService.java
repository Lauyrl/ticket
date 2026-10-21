package com.example.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.order.CreateOrderRequest;
import com.example.ticket.dto.order.OrderItemResponse;
import com.example.ticket.dto.order.OrderResponse;
import com.example.ticket.dto.order.OrderSummaryResponse;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.event.SeatHoldEntity;
import com.example.ticket.entity.order.OrderEntity;
import com.example.ticket.entity.order.OrderItemEntity;
import com.example.ticket.enums.OrderStatus;
import com.example.ticket.enums.SeatHoldStatus;
import com.example.ticket.exceptions.HoldExpiredException;
import com.example.ticket.exceptions.ItemNotFoundException;
import com.example.ticket.exceptions.OrderAlreadyExistsException;
import com.example.ticket.exceptions.OrderNotPendingException;
import com.example.ticket.exceptions.SeatNotHeldByUserException;
import com.example.ticket.repository.event.SeatHoldRepository;
import com.example.ticket.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final SeatHoldRepository seatHoldRepository;

    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        SeatHoldEntity hold = seatHoldRepository.findById(request.getHoldId())
                .orElseThrow(() -> new ItemNotFoundException("Seat hold not found with id: " + request.getHoldId()));

        if (!hold.getUser().getId().equals(userId)) {
            throw new SeatNotHeldByUserException("Seat hold does not belong to user: " + userId);
        }

        Instant now = Instant.now();
        if (hold.getStatus() != SeatHoldStatus.ACTIVE) {
            throw new HoldExpiredException("Seat hold is not ACTIVE (current status: " + hold.getStatus() + ")");
        }

        if (hold.getExpiresAt().isBefore(now)) {
            throw new HoldExpiredException("Seat hold has expired at: " + hold.getExpiresAt());
        }

        if (orderRepository.existsByHoldIdAndStatus(hold.getId(), OrderStatus.PENDING)) {
            throw new OrderAlreadyExistsException("A pending order already exists for hold id: " + hold.getId());
        }

        SeatEntity seat = hold.getSeat();
        EventEntity event = seat.getEvent();
        BigDecimal unitPrice = seat.getZone().getPrice();

        OrderEntity order = OrderEntity.builder()
                .orderCode(generateOrderCode())
                .user(hold.getUser())
                .event(event)
                .hold(hold)
                .status(OrderStatus.PENDING)
                .totalAmount(unitPrice)
                .expiresAt(hold.getExpiresAt())
                .build();

        OrderItemEntity item = OrderItemEntity.builder()
                .order(order)
                .seat(seat)
                .zoneName(seat.getZone().getName())
                .rowLabel(formatRowLabel(seat.getRowIndex()))
                .seatNumber(seat.getSeatNumber())
                .unitPrice(unitPrice)
                .build();

        order.addItem(item);

        OrderEntity savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin) {
        OrderEntity order = isAdmin
                ? orderRepository.findByIdWithItems(orderId)
                        .orElseThrow(() -> new ItemNotFoundException("Order not found with id: " + orderId))
                : orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                        .orElseThrow(() -> new ItemNotFoundException("Order not found with id: " + orderId));

        // Check if pending order has expired
        if (order.getStatus() == OrderStatus.PENDING && order.getExpiresAt().isBefore(Instant.now())) {
            order.setStatus(OrderStatus.EXPIRED);
            order = orderRepository.save(order);
        }

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page = (status != null)
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);

        return page.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(Long eventId, OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page;
        if (eventId != null && status != null) {
            page = orderRepository.findByEventIdAndStatus(eventId, status, pageable);
        } else if (eventId != null) {
            page = orderRepository.findByEventId(eventId, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }

        return page.map(OrderSummaryResponse::from);
    }

    public OrderResponse payOrder(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException("Only PENDING orders can be paid (current status: " + order.getStatus() + ")");
        }

        if (order.getExpiresAt().isBefore(Instant.now())) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            throw new HoldExpiredException("Order has expired at: " + order.getExpiresAt());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());

        SeatHoldEntity hold = order.getHold();
        if (hold != null) {
            hold.setStatus(SeatHoldStatus.CONVERTED);
            seatHoldRepository.save(hold);
        }

        OrderEntity savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    public void cancelOrder(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException("Only PENDING orders can be cancelled (current status: " + order.getStatus() + ")");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Release the seat hold
        SeatHoldEntity hold = order.getHold();
        if (hold != null && hold.getStatus() == SeatHoldStatus.ACTIVE) {
            hold.setStatus(SeatHoldStatus.RELEASED);
            seatHoldRepository.save(hold);
        }

        orderRepository.save(order);
    }

    private String generateOrderCode() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "TKR-" + datePart + "-" + randomPart;
    }

    public static String formatRowLabel(Integer rowIndex) {
        if (rowIndex == null || rowIndex <= 0) {
            return "A";
        }
        StringBuilder sb = new StringBuilder();
        int temp = rowIndex;
        while (temp > 0) {
            temp--;
            sb.insert(0, (char) ('A' + (temp % 26)));
            temp /= 26;
        }
        return sb.toString();
    }
}
