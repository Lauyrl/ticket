package com.example.ticket.exceptions;

public class TicketNotFoundException extends ItemNotFoundException {
    public TicketNotFoundException(Long id) {
        super("Ticket not found: " + id);
    }
}
