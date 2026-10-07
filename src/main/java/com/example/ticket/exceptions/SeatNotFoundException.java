package com.example.ticket.exceptions;

public class SeatNotFoundException extends ItemNotFoundException {
    public SeatNotFoundException(Long seatId) {
        super("Event not found: " + seatId.toString());
    }
}
