package com.example.ticket.exceptions;

public class OrderAlreadyExistsException extends RuntimeException {
    public OrderAlreadyExistsException(String message) {
        super(message);
    }
}
