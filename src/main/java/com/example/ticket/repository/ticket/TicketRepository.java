
package com.example.ticket.repository.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticket.entity.ticket.TicketEntity;
import com.example.ticket.enums.TicketStatus;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Page<TicketEntity> findByUserId(Long userId, Pageable pageable);
    Optional<TicketEntity> findByIdAndUserId(Long ticketId, Long userId);

    Page<TicketEntity> findByUserIdAndStatus(
            Long userId,
            TicketStatus status,
            Pageable pageable);
    
    Page<TicketEntity> findByUserIdAndEventId(
        Long userId,
        Long eventId,
        Pageable pageable);
}
