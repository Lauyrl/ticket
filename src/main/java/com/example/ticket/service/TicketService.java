
package com.example.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;
import com.example.ticket.repository.ticket.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public Page<TicketEntity> getMyTickets(
            Long userId,
            TicketStatus status,
            Pageable pageable) {

        if (status != null) {
            return ticketRepository.findByUserIdAndStatus(
                    userId, status, pageable);
        }

        return ticketRepository.findByUserId(userId, pageable);
    }
}
