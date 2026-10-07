package com.example.ticket.repository.event;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.ticket.dto.event.SeatWithStatusObject;
import com.example.ticket.entity.event.SeatEntity;


public interface SeatRepository extends JpaRepository<SeatEntity, Long> { 
    
    List<SeatEntity> findByEventId(Long eventId);

    List<SeatEntity> findByZoneId(Long zoneId);

    @Query(value = """
        SELECT 
            s.id AS id,
            s.zone_id AS zoneId, 
            s.row_index AS rowIndex, 
            s.seat_number AS seatNumber,
            CASE 
                WHEN h.status = 'CONVERTED'   THEN 'SOLD'
                --- WHEN h.user_id = :userId  THEN 'HELD_BY_ME'
                WHEN h.id IS NOT NULL         THEN 'HELD'
                ELSE                               'AVAILABLE'
            END AS status
        FROM event_seats s
        LEFT JOIN seat_holds h ON h.seat_id = s.id
                              AND (
                                h.status = 'CONVERTED' 
                                OR                      --- only 1 hold can ever be CONVERTED/ACTIVE due to UNIQUE INDEX ux_seat_holds_taken_seat   
                                (h.status = 'ACTIVE' AND h.expires_at > now())
                              ) --- ignore RELEASED (inactive) holds
        WHERE s.event_id = :eventId
        ORDER BY s.zone_id, s.row_index, s.seat_number
        """, nativeQuery = true)
    List<SeatWithStatusObject> findSeatsWithStatusByEventId(Long eventId); //, @Param("userId") Long userId
    // Spring Data can make proxy objects at runtime that implement SeatWithStatusObject
    // proxy object getters must match column aliases
}
