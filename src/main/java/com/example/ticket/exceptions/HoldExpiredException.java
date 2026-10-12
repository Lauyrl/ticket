package com.example.ticket.exceptions;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
