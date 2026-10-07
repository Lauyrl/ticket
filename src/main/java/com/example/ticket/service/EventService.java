package com.example.ticket.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.event.EventResponse;
import com.example.ticket.dto.event.SeatHoldResponse;
import com.example.ticket.dto.event.SeatResponse;
import com.example.ticket.dto.event.SeatWithStatusObject;
import com.example.ticket.dto.event.SeatZoneResponse;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.event.SeatHoldEntity;
import com.example.ticket.entity.event.SeatZoneEntity;
import com.example.ticket.exceptions.EventNotFoundException;
import com.example.ticket.exceptions.SeatNotFoundException;
import com.example.ticket.exceptions.SeatNotHeldByUserException;
import com.example.ticket.exceptions.SeatUnavailableException;
import com.example.ticket.repository.event.EventRepository;
import com.example.ticket.repository.event.SeatHoldRepository;
import com.example.ticket.repository.event.SeatRepository;
import com.example.ticket.repository.event.SeatZoneRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
@Transactional(readOnly = true) // reduce Hibernate overhead for read only methods
public class EventService {
    private static final int PG_LEN = 12;

    private final EventRepository eventRepository;
    private final SeatZoneRepository seatZoneRepository;
    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;

    public Page<EventResponse> getEvents(String search, String city, Instant startsAfter, Instant startsBefore, Instant endsAfter, Instant endsBefore, int page) {
        Pageable pageable = PageRequest.of(page, PG_LEN);
        Page<EventEntity> events = eventRepository.findEvents(search, city, startsAfter, startsBefore, endsAfter, endsBefore, pageable);

        return events.map(EventResponse::new);
    }

    public EventResponse getEventById(Long eventId) {
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId)); 
       
        return new EventResponse(event);  
    }

    public List<EventResponse> getSuggestedEvents(String search) {
        List<EventEntity> suggestedEvents = eventRepository.findSuggestedEvents(search);

        return suggestedEvents.stream().map(EventResponse::new).toList();
    }

    public List<SeatZoneResponse> getSeatsByEvent(Long eventId) {
        List<SeatZoneEntity> zones = seatZoneRepository.findByEventId(eventId);

        Map<Long, List<SeatWithStatusObject>> seatsByZone = seatRepository
                .findSeatsWithStatusByEventId(eventId).stream()
                .collect(Collectors.groupingBy(seat -> seat.getZoneId()));

        return zones.stream()
                .map(zone -> new SeatZoneResponse(
                    zone, 
                    seatsByZone.getOrDefault(zone.getId(), List.of())
                        .stream()
                        .map(SeatResponse::new)
                        .toList()
                ))
                .toList();
    }

    @Transactional // non-readOnly
    public SeatHoldResponse holdSeat(Long eventId, Long seatId) { // , Long userId
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        SeatEntity seat = seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));

        // the DB can't check if the seat actually belongs to this event, so check here
        if (!seat.getEvent().getId().equals(eventId)) {
            throw new SeatNotFoundException(seatId);
        }

        // TODO: seat cap if ACTIVE or CONVERTED holds for this user in this event >= event.getMaxSeatsPerUser()

        Instant now = Instant.now(); //  snapshot now()
        // expire stale ACTIVE holds via lazy check
        seatHoldRepository.expireStaleHolds(seatId, now);

        SeatHoldEntity hold = new SeatHoldEntity(
            seat,
            now.plus(Duration.ofMinutes(event.getHoldDurationMinutes()))
            //, currentUserId
        );

        try {
            // Hibernate might buffer this query until the method returns so flush manually
            seatHoldRepository.saveAndFlush(hold);
        } catch (DataIntegrityViolationException e) {
            // Spring turns constraint-based DB error (ux_seat_holds_taken_seat) into DataIntegrityViolationException
            throw new SeatUnavailableException(seatId);
        }

        return new SeatHoldResponse(hold);
    }

    @Transactional // non-readOnly
    public void releaseSeat(Long eventId, Long seatId) { // , Long userId
        SeatEntity seat = seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));

        // the DB can't check if the seat actually belongs to this event, so check here
        if (!seat.getEvent().getId().equals(eventId)) {
            throw new SeatNotFoundException(seatId);
        }

        int released = seatHoldRepository.releaseActiveHold(seatId, Instant.now()); // userId
        if (released == 0) {
            throw new SeatNotHeldByUserException(seatId);   // currentUser has no active hold on this seat
        }
    }

    public List<SeatHoldResponse> getUserSeatHolds(Long eventId) { //, Long userId
        List<SeatHoldEntity> holds = seatHoldRepository.findActiveByEventIdAndUserId(eventId, Instant.now());

        return holds.stream()
            .map(SeatHoldResponse::new)
            .toList();
    }
}
