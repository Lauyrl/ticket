package com.example.ticket.exceptions;

public class SeatNotHeldByUserException extends RuntimeException {
    public SeatNotHeldByUserException(Long seatId) {
        super("You do not currently hold this seat: " + seatId.toString());
    }
}
