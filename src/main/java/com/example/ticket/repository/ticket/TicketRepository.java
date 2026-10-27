package com.example.ticket.repository.ticket;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Page<TicketEntity> findByUserId(Long userId, Pageable pageable);

    Page<TicketEntity> findByUserIdAndStatus(Long userId, TicketStatus status, Pageable pageable);

    Page<TicketEntity> findByUserIdAndEventId(Long userId, Long eventId, Pageable pageable);

    Page<TicketEntity> findByUserIdAndEventIdAndStatus(Long userId, Long eventId, TicketStatus status, Pageable pageable);

    Optional<TicketEntity> findByIdAndUserId(Long ticketId, Long userId);
}