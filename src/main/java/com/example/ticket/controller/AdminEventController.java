package com.example.ticket.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.event.CreateEventRequest;
import com.example.ticket.dto.event.CreateSeatZoneRequest;
import com.example.ticket.dto.event.EventResponse;
import com.example.ticket.dto.event.SeatZoneResponse;
import com.example.ticket.dto.event.UpdateEventRequest;
import com.example.ticket.enums.EventStatus;
import com.example.ticket.security.UserPrincipal;
import com.example.ticket.service.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public Page<EventResponse> getAllEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return eventService.getAdminEvents(search, status, pageRequest);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long adminId = currentUser != null ? currentUser.getId() : null;
        return eventService.createEvent(request, adminId);
    }

    @PatchMapping("/{eventId}")
    public EventResponse updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(eventId, request);
    }

    @PostMapping("/{eventId}/seat-zones")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatZoneResponse> createSeatZones(
            @PathVariable Long eventId,
            @Valid @RequestBody List<CreateSeatZoneRequest> requests) {
        return eventService.setEventSeatZones(eventId, requests);
    }
}
