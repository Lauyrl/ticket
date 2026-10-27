package com.example.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.ticket.dto.ticket.TicketResponse;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.order.OrderEntity;
import com.example.ticket.entity.order.OrderItemEntity;
import com.example.ticket.entity.user.UserEntity;
import com.example.ticket.enums.OrderStatus;
import com.example.ticket.exceptions.TicketNotFoundException;
import com.example.ticket.repository.order.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private TicketService ticketService;

    private UserEntity user;
    private EventEntity event;
    private OrderEntity order;
    private OrderItemEntity item;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).email("user@example.com").build();
        event = new EventEntity();
        event.setId(10L);
        event.setName("Rock Concert");
        event.setVenue("Stadium");
        event.setCity("Hanoi");
        event.setStartsAt(Instant.now().plusSeconds(3600));

        order = OrderEntity.builder()
                .id(100L)
                .orderCode("ORD-123")
                .user(user)
                .event(event)
                .status(OrderStatus.PAID)
                .paidAt(Instant.now())
                .build();
        item = OrderItemEntity.builder()
                .id(50L)
                .order(order)
                .zoneName("VIP")
                .rowLabel("A")
                .seatNumber(1)
                .unitPrice(new BigDecimal("100000"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getMyTickets_WithValidStatus_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderItemRepository.findPaidTickets(1L, 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        Page<TicketResponse> result = ticketService.getMyTickets(1L, 10L, "VALID", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("ORD-123-T50", result.getContent().get(0).ticketCode());
        assertEquals("VALID", result.getContent().get(0).status());
    }

    @Test
    void getMyTickets_WithUsedStatus_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<TicketResponse> result = ticketService.getMyTickets(1L, 10L, "USED", pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getTicketById_UserOwner_Success() {
        when(orderItemRepository.findPaidTicketById(50L, 1L)).thenReturn(Optional.of(item));

        TicketResponse response = ticketService.getTicketById(50L, 1L);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertEquals("ORD-123-T50", response.ticketCode());
    }

    @Test
    void getTicketById_NotFound_ThrowsException() {
        when(orderItemRepository.findPaidTicketById(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(99L, 1L));
    }

    @Test
    void getTicketById_NotOwner_ThrowsException() {
        when(orderItemRepository.findPaidTicketById(50L, 2L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(50L, 2L));
    }
}
