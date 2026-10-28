package com.example.ticket.exceptions;

public class SeatNotFoundException extends ItemNotFoundException {
    public SeatNotFoundException(Long seatId) {
        super("Seat not found: " + seatId.toString());
    }
}
