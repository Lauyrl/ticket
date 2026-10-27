package com.example.ticket.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.ticket.TicketResponse;
import com.example.ticket.enums.TicketStatus;
import com.example.ticket.security.UserPrincipal;
import com.example.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/my")
    public Page<TicketResponse> getMyTickets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "issuedAt"));
        return ticketService.getMyTickets(currentUser.getId(), eventId, status, pageable);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getMyTicketById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long ticketId) {

        return ticketService.getMyTicketById(currentUser.getId(), ticketId);
    }
}