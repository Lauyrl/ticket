package com.example.ticket.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.event.EventResponse;
import com.example.ticket.dto.event.SeatHoldResponse;
import com.example.ticket.dto.event.SeatZoneResponse;
import com.example.ticket.service.EventService;

import jakarta.validation.constraints.Min;
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
        @RequestParam(defaultValue = "0") @Min(0) int page
    ) {
        Page<EventResponse> response = eventService.getEvents(
            search, city, startsAfter, startsBefore, endsAfter, endsBefore, page
        );
        return response;
    }

    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getEventById(@PathVariable Long eventId) {
        EventResponse response = eventService.getEventById(eventId);
        return response;
    }

    @GetMapping("/api/v1/events/suggest")
    public List<EventResponse> getSuggestedEvents(@RequestParam(required = false) String search) {
        List<EventResponse> response = eventService.getSuggestedEvents(search);
        return response;
    }

    @GetMapping("/api/v1/events/{eventId}/seats")
    public List<SeatZoneResponse> getSeatsByEvent(@PathVariable Long eventId) {
        List<SeatZoneResponse> response = eventService.getSeatsByEvent(eventId);
        return response;
    }

    @PostMapping("/api/v1/events/{eventId}/seats/{seatId}/hold")
    public SeatHoldResponse holdSeat(@PathVariable Long eventId, @PathVariable Long seatId) {
        return eventService.holdSeat(eventId, seatId);
    }

    @DeleteMapping("/api/v1/events/{eventId}/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseSeat(@PathVariable Long eventId, @PathVariable Long seatId) {
        eventService.releaseSeat(eventId, seatId);   // add userId once auth exists
    }

    @GetMapping("/api/v1/events/{eventId}/holds")
    public List<SeatHoldResponse> getUserSeatHolds(@PathVariable Long eventId) {
        List<SeatHoldResponse> response = eventService.getUserSeatHolds(eventId);
        return response;
    }
}
