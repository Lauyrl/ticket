package com.example.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ticket.dto.order.CreateOrderRequest;
import com.example.ticket.dto.order.OrderResponse;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.event.SeatHoldEntity;
import com.example.ticket.entity.event.SeatZoneEntity;
import com.example.ticket.entity.order.OrderEntity;
import com.example.ticket.entity.user.UserEntity;
import com.example.ticket.enums.OrderStatus;
import com.example.ticket.enums.SeatHoldStatus;
import com.example.ticket.enums.UserRole;
import com.example.ticket.exceptions.HoldExpiredException;
import com.example.ticket.exceptions.ItemNotFoundException;
import com.example.ticket.exceptions.OrderAlreadyExistsException;
import com.example.ticket.exceptions.OrderNotPendingException;
import com.example.ticket.exceptions.SeatNotHeldByUserException;
import com.example.ticket.repository.event.SeatHoldRepository;
import com.example.ticket.repository.order.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @InjectMocks
    private OrderService orderService;

    private UserEntity user;
    private EventEntity event;
    private SeatZoneEntity zone;
    private SeatEntity seat;
    private SeatHoldEntity hold;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .email("customer@ticketrush.com")
                .fullName("Customer One")
                .role(UserRole.CUSTOMER)
                .build();

        event = new EventEntity();
        event.setId(10L);
        event.setName("Rock Concert");

        zone = new SeatZoneEntity();
        zone.setId(20L);
        zone.setName("VIP Zone");
        zone.setPrice(new BigDecimal("1500000.00"));

        seat = new SeatEntity();
        seat.setId(100L);
        seat.setEvent(event);
        seat.setZone(zone);
        seat.setRowIndex(1);
        seat.setSeatNumber(5);

        hold = new SeatHoldEntity();
        hold.setId(55L);
        hold.setUser(user);
        hold.setSeat(seat);
        hold.setStatus(SeatHoldStatus.ACTIVE);
        hold.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    @Test
    void testCreateOrderSuccess() {
        CreateOrderRequest request = new CreateOrderRequest(55L);

        when(seatHoldRepository.findById(55L)).thenReturn(Optional.of(hold));
        when(orderRepository.existsByHoldIdAndStatus(55L, OrderStatus.PENDING)).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderResponse response = orderService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(10L, response.getEventId());
        assertEquals("Rock Concert", response.getEventName());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("1500000.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals("VIP Zone", response.getItems().get(0).getZoneName());
        assertEquals("A", response.getItems().get(0).getRowLabel());
        assertEquals(5, response.getItems().get(0).getSeatNumber());
    }

    @Test
    void testCreateOrderHoldNotBelongToUser() {
        CreateOrderRequest request = new CreateOrderRequest(55L);
        when(seatHoldRepository.findById(55L)).thenReturn(Optional.of(hold));

        assertThrows(SeatNotHeldByUserException.class, () -> orderService.createOrder(999L, request));
    }

    @Test
    void testCreateOrderHoldExpired() {
        hold.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        CreateOrderRequest request = new CreateOrderRequest(55L);
        when(seatHoldRepository.findById(55L)).thenReturn(Optional.of(hold));

        assertThrows(HoldExpiredException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void testCreateOrderAlreadyPending() {
        CreateOrderRequest request = new CreateOrderRequest(55L);
        when(seatHoldRepository.findById(55L)).thenReturn(Optional.of(hold));
        when(orderRepository.existsByHoldIdAndStatus(55L, OrderStatus.PENDING)).thenReturn(true);

        assertThrows(OrderAlreadyExistsException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void testCancelOrderSuccess() {
        OrderEntity order = OrderEntity.builder()
                .id(1L)
                .orderCode("TKR-20261012-ABC123")
                .user(user)
                .hold(hold)
                .status(OrderStatus.PENDING)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        when(orderRepository.findByIdAndUserIdWithItems(1L, 1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L, 1L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(SeatHoldStatus.RELEASED, hold.getStatus());
        verify(seatHoldRepository).save(hold);
        verify(orderRepository).save(order);
    }

    @Test
    void testCancelOrderNotPending() {
        OrderEntity order = OrderEntity.builder()
                .id(1L)
                .orderCode("TKR-20261012-ABC123")
                .user(user)
                .hold(hold)
                .status(OrderStatus.PAID)
                .build();

        when(orderRepository.findByIdAndUserIdWithItems(1L, 1L)).thenReturn(Optional.of(order));

        assertThrows(OrderNotPendingException.class, () -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    void testGetOrderNotFound() {
        when(orderRepository.findByIdAndUserIdWithItems(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.getOrderById(999L, 1L, false));
    }
}
