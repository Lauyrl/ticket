package com.example.ticket.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticket.dto.event.CreateEventRequest;
import com.example.ticket.dto.event.CreateSeatRequest;
import com.example.ticket.dto.event.CreateSeatZoneRequest;
import com.example.ticket.dto.event.EventResponse;
import com.example.ticket.dto.event.SeatHoldResponse;
import com.example.ticket.dto.event.SeatResponse;
import com.example.ticket.dto.event.SeatWithStatusObject;
import com.example.ticket.dto.event.SeatZoneResponse;
import com.example.ticket.dto.event.UpdateEventRequest;
import com.example.ticket.entity.event.EventEntity;
import com.example.ticket.entity.event.SeatEntity;
import com.example.ticket.entity.event.SeatHoldEntity;
import com.example.ticket.entity.event.SeatZoneEntity;
import com.example.ticket.enums.EventStatus;
import com.example.ticket.exceptions.EventNotFoundException;
import com.example.ticket.exceptions.SeatNotFoundException;
import com.example.ticket.exceptions.SeatNotHeldByUserException;
import com.example.ticket.exceptions.SeatUnavailableException;
import com.example.ticket.repository.event.EventRepository;
import com.example.ticket.repository.event.SeatHoldRepository;
import com.example.ticket.repository.event.SeatRepository;
import com.example.ticket.repository.event.SeatZoneRepository;

import jakarta.validation.Valid;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final com.example.ticket.repository.user.UserRepository userRepository;

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

    public List<SeatZoneResponse> getSeatsByEvent(Long eventId, Long userId) {
        List<SeatZoneEntity> zones = seatZoneRepository.findByEventId(eventId);

        Map<Long, List<SeatWithStatusObject>> seatsByZone = seatRepository
                .findSeatsWithStatusByEventId(eventId, userId).stream()
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

    // ADMIN
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        EventEntity event = new EventEntity(request.name(), request.description(), request.category(), request.venue(), request.city(), request.startsAt(), request.endsAt(), request.imageUrl(), request.status(), request.maxSeatsPerUser(), request.holdDurationMinutes());
        eventRepository.save(event);

        List<SeatZoneEntity> zones = request.zones()
            .stream()
            .map(zoneRequest -> {
                SeatZoneEntity zone = new SeatZoneEntity(zoneRequest.name(), zoneRequest.price(), event); 
                return zone;
            })
            .toList();
        // zones need to exist before seats to create FKs
        seatZoneRepository.saveAll(zones);

        List<SeatEntity> seats = request.zones()
            .stream()
            .flatMap(zoneRequest -> {
                return zoneRequest.seats()
                    .stream()
                    .map(seatRequest -> new SeatEntity(
                        seatRequest.rowIndex(), 
                        seatRequest.seatNumber(), 
                        event, 
                        zones.get(request.zones().indexOf(zoneRequest))
                    )); // return the stream
            }) // flatMap merges all streams returned
            .toList();
        seatRepository.saveAll(seats);

        return new EventResponse(event);
    }

    // ADMIN
    @Transactional
    public void updateEvent(Long eventId, UpdateEventRequest request) {
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        if (request.name() != null) event.setName(request.name());
        if (request.description() != null) event.setDescription(request.description());
        if (request.category() != null) event.setCategory(request.category());
        if (request.venue() != null) event.setVenue(request.venue());
        if (request.city() != null) event.setCity(request.city());
        if (request.startsAt() != null) event.setStartsAt(request.startsAt());
        if (request.endsAt() != null) event.setEndsAt(request.endsAt());
        if (request.imageUrl() != null) event.setImageUrl(request.imageUrl());
        if (request.status() != null) event.setStatus(request.status());
        if (request.maxSeatsPerUser() != null) event.setMaxSeatsPerUser(request.maxSeatsPerUser());
        if (request.holdDurationMinutes() != null) event.setHoldDurationMinutes(request.holdDurationMinutes());

        // additional check after Controller-layer check, for partial updates which need to compare existing timestamps with new ones
        if (event.getStartsAt() != null 
            && event.getEndsAt() != null
            && !event.getEndsAt().isAfter(event.getStartsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }

    // ADMIN
    // public List<SeatZoneResponse> setEventSeatZones(Long eventId, List<CreateSeatZoneRequest> newZonesRequests) {
    //     Set<SeatZoneEntity> allExistingZones = new HashSet<>(seatZoneRepository.findAllByEventId(eventId));
        
    //     // if zone in list matches existing zone by (eventId, name), update matching existing zones 
    //     List<SeatZoneEntity> matchingExistingZones = seatZoneRepository.findAllByEventIdAndName(
    //         eventId, newZonesRequests.stream().map(z -> z.name()).toList() 
    //     );

    //     for (SeatZoneEntity zone : matchingExistingZones) {
    //         // the only updateable field
    //         zone.setPrice(newZonesRequests.get(matchingExistingZones.indexOf(zone)).price());
    //     }

    //     // if zone in list doesnt match any existing zone -> create new zone
    //     List<SeatZoneEntity> newZones = newZonesRequests
    //         .stream()
    //         .filter(e -> )

    //     // if an existing zone not in list -> delete

    // }

    @Transactional // non-readOnly
    public SeatHoldResponse holdSeat(Long eventId, Long seatId, Long userId) {
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        SeatEntity seat = seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));
        com.example.ticket.entity.user.UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // the DB can't check if the seat actually belongs to this event, so check here
        if (!seat.getEvent().getId().equals(eventId)) {
            throw new SeatNotFoundException(seatId);
        }

        Instant now = Instant.now(); // snapshot now()

        // seat cap: check if ACTIVE or CONVERTED holds for this user in this event >= event.getMaxSeatsPerUser()
        long heldCount = seatHoldRepository.countHeldOrPurchasedSeatsByUser(eventId, userId, now);
        if (heldCount >= event.getMaxSeatsPerUser()) {
            throw new com.example.ticket.exceptions.MaxSeatsExceededException(event.getMaxSeatsPerUser());
        }

        // expire stale ACTIVE holds via lazy check
        seatHoldRepository.expireStaleHolds(seatId, now);

        SeatHoldEntity hold = new SeatHoldEntity(
            seat,
            user,
            now.plus(Duration.ofMinutes(event.getHoldDurationMinutes()))
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
    public void releaseSeat(Long eventId, Long seatId, Long userId) {
        SeatEntity seat = seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));

        // the DB can't check if the seat actually belongs to this event, so check here
        if (!seat.getEvent().getId().equals(eventId)) {
            throw new SeatNotFoundException(seatId);
        }

        int released = seatHoldRepository.releaseActiveHold(seatId, userId, Instant.now());
        if (released == 0) {
            throw new SeatNotHeldByUserException(seatId);   // currentUser has no active hold on this seat
        }
    }

    public List<SeatHoldResponse> getUserSeatHolds(Long eventId, Long userId) {
        List<SeatHoldEntity> holds = seatHoldRepository.findActiveByEventIdAndUserId(eventId, userId, Instant.now());

        return holds.stream()
            .map(SeatHoldResponse::new)
            .toList();
    }
}
