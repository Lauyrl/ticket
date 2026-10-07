package com.example.ticket.exceptions;

public class EventNotFoundException extends ItemNotFoundException {
    public EventNotFoundException(Long id) {
        super("Event not found: " + id.toString());
    }
}
