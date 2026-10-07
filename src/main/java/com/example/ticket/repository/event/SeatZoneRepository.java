package com.example.ticket.repository.event;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ticket.entity.event.SeatZoneEntity;

public interface SeatZoneRepository extends JpaRepository<SeatZoneEntity, Long> {
    
    List<SeatZoneEntity> findByEventId(Long eventId);
}
