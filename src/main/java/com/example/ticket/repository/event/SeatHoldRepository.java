package com.example.ticket.repository.event;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.ticket.entity.event.SeatHoldEntity;

public interface SeatHoldRepository extends JpaRepository<SeatHoldEntity, Long> {
    
    @Modifying()
    @Query("""
        UPDATE SeatHoldEntity h 
        SET h.status = com.example.ticket.enums.SeatHoldStatus.RELEASED
        WHERE h.seat.id = :seatId 
          AND h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE AND h.expiresAt < :now
        """)
    void expireStaleHolds(Long seatId, Instant now); // lazy update

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE SeatHoldEntity h
        SET h.status = com.example.ticket.enums.SeatHoldStatus.RELEASED
        WHERE h.seat.id = :seatId
          AND h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE
          AND h.expiresAt > :now
        """)
    //--- AND h.userId = :userId
    int releaseActiveHold(Long seatId, Instant now); //, Long userId
    // only check unexpired holds
    // returns number of rows affected

    @Query("""
        SELECT h 
        FROM SeatHoldEntity h
        JOIN FETCH h.seat s
        WHERE s.event.id = :eventId
          AND h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE
          AND h.expiresAt > :now
        ORDER BY h.expiresAt
        """)
    //--- AND h.userId = :userId
    List<SeatHoldEntity> findActiveByEventIdAndUserId(Long eventId, Instant now); //, Long userId
}
