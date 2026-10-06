package com.example.ticket.dto;

import java.time.Instant;

import com.example.ticket.entity.EventEntity;
import com.example.ticket.enums.EventStatus;

public record EventResponse(
    Long id, 
    String name,
    String description,
    String category,
    String venue,
    String city,
    Instant startsAt,
    Instant endsAt,
    String imageUrl,
    EventStatus status,
    int maxSeatsPerUser,
    int holdDurationMinutes
) {
    public EventResponse(EventEntity e) {
        this(
            e.getId(), 
            e.getName(), 
            e.getDescription(), 
            e.getCategory(), 
            e.getVenue(), 
            e.getCity(), 
            e.getStartsAt(), 
            e.getEndsAt(), 
            e.getImageUrl(), 
            e.getStatus(),
            e.getMaxSeatsPerUser(), 
            e.getHoldDurationMinutes()
        );
    }
}
