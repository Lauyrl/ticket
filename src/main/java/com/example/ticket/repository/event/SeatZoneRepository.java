package com.example.ticket.repository.event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticket.entity.event.SeatZoneEntity;

public interface SeatZoneRepository extends JpaRepository<SeatZoneEntity, Long> {
    
    List<SeatZoneEntity> findByEventId(Long eventId);

    Optional<SeatZoneEntity> findByEventIdAndName(Long eventId, String name);
}
