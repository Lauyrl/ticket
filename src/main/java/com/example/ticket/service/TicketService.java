package com.example.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.ticket.TicketResponse;
import com.example.ticket.entity.order.OrderItemEntity;
import com.example.ticket.exceptions.TicketNotFoundException;
import com.example.ticket.repository.order.OrderItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final OrderItemRepository orderItemRepository;

    public Page<TicketResponse> getMyTickets(Long userId, Long eventId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            if (!"VALID".equalsIgnoreCase(status.trim())) {
                return Page.empty(pageable);
            }
        }
        return orderItemRepository.findPaidTickets(userId, eventId, pageable)
                .map(TicketResponse::from);
    }

    public TicketResponse getTicketById(Long ticketId, Long userId) {
        OrderItemEntity item = orderItemRepository.findPaidTicketById(ticketId, userId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return TicketResponse.from(item);
    }
}