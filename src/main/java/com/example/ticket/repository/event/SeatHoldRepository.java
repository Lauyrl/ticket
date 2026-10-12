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
          AND h.user.id = :userId
          AND h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE
          AND h.expiresAt > :now
        """)
    int releaseActiveHold(Long seatId, Long userId, Instant now);
    // only check unexpired holds of this user
    // returns number of rows affected

    @Query("""
        SELECT h 
        FROM SeatHoldEntity h
        JOIN FETCH h.seat s
        WHERE s.event.id = :eventId
          AND h.user.id = :userId
          AND h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE
          AND h.expiresAt > :now
        ORDER BY h.expiresAt
        """)
    List<SeatHoldEntity> findActiveByEventIdAndUserId(Long eventId, Long userId, Instant now);

    @Query("""
        SELECT COUNT(h)
        FROM SeatHoldEntity h
        WHERE h.seat.event.id = :eventId
          AND h.user.id = :userId
          AND (
            h.status = com.example.ticket.enums.SeatHoldStatus.CONVERTED
            OR (h.status = com.example.ticket.enums.SeatHoldStatus.ACTIVE AND h.expiresAt > :now)
          )
        """)
    long countHeldOrPurchasedSeatsByUser(Long eventId, Long userId, Instant now);
}
