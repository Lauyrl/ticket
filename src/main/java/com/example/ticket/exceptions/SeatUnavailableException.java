package com.example.ticket.exceptions;

public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(Long seatId) {
        super("This seat is not available: " + seatId.toString());
    }
}
