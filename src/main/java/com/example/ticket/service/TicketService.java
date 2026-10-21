package com.example.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.ticket.TicketResponse;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;
import com.example.ticket.exceptions.ItemNotFoundException;
import com.example.ticket.repository.event.EventRepository;
import com.example.ticket.repository.event.SeatRepository;
import com.example.ticket.repository.ticket.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    public Page<TicketResponse> getMyTickets(Long userId, Long eventId, TicketStatus status, Pageable pageable) {
        Page<TicketEntity> page;

        if (eventId != null && status != null) {
            page = ticketRepository.findByUserIdAndEventIdAndStatus(userId, eventId, status, pageable);
        } else if (eventId != null) {
            page = ticketRepository.findByUserIdAndEventId(userId, eventId, pageable);
        } else if (status != null) {
            page = ticketRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = ticketRepository.findByUserId(userId, pageable);
        }

        return page.map(this::toResponse);
    }

    public TicketResponse getMyTicketById(Long userId, Long ticketId) {
        TicketEntity ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Ticket not found"));
        return toResponse(ticket);
    }

    private TicketResponse toResponse(TicketEntity ticket) {
        SeatEntity seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new ItemNotFoundException("Seat not found"));
        EventEntity event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));
        return TicketResponse.of(ticket, seat, event.getName());
    }
}