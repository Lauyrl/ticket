package com.example.ticket.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.ticket.TicketResponse;
import com.example.ticket.enums.UserRole;
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
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return ticketService.getMyTickets(currentUser.getId(), eventId, status, pageRequest);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicketById(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        return ticketService.getTicketById(ticketId, currentUser.getId(), isAdmin);
    }
}
