package com.example.ticket.exceptions;

public class MaxSeatsExceededException extends RuntimeException {
    public MaxSeatsExceededException(int maxSeats) {
        super("Maximum held/purchased seats per user reached for this event (" + maxSeats + " seats)");
    }
}
