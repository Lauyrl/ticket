package com.example.ticket.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.EventResponse;
import com.example.ticket.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor 
public class EventController {
    private final EventService eventService;
    
    @GetMapping("/api/v1/events")
    public Page<EventResponse> getEvents(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Instant startsAfter,
        @RequestParam(required = false) Instant startsBefore,
        @RequestParam(required = false) Instant endsAfter,
        @RequestParam(required = false) Instant endsBefore,
        @RequestParam(defaultValue = "0") int page
    ) {
        Page<EventResponse> response = eventService.getEvents(
            search, city, startsAfter, startsBefore, endsAfter, endsBefore, page
        );
        return response;
    }

    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getSuggestedEvents(@PathVariable Long eventId) {
        EventResponse response = eventService.getEventById(eventId);
        return response;
    }

    @GetMapping("/api/v1/events/suggest")
    public List<EventResponse> getSuggestedEvents(String search) {
        List<EventResponse> response = eventService.getSuggestedEvents(search);
        return response;
    }

}
