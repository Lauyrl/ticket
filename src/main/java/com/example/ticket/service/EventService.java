package com.example.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.ticket.dto.EventResponse;
import com.example.ticket.entity.EventEntity;
import com.example.ticket.exceptions.EventNotFoundException;
import com.example.ticket.repository.EventRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
@Transactional 
public class EventService {
    private static final int PG_LEN = 12;
    public final EventRepository eventRepository;

    public Page<EventResponse> getEvents(String search, String city, Instant startsAfter, Instant startsBefore, Instant endsAfter, Instant endsBefore, int page) {
        Pageable pageable = PageRequest.of(page, PG_LEN);
        Page<EventEntity> events = eventRepository.findEvents(search, city, startsAfter, startsBefore, endsAfter, endsBefore, pageable);

        return events.map(EventResponse::new);
    }

    public EventResponse getEventById(Long eventId) throws NoSuchElementException {
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId)); 
       
        return new EventResponse(event);  
    }

    public List<EventResponse> getSuggestedEvents(String search) {
        List<EventEntity> suggestedEvents = eventRepository.findSuggestedEvents(search);

        return suggestedEvents.stream().map(EventResponse::new).toList();
    }
}
