package com.example.ticket.exceptions;

public class OrderNotPendingException extends RuntimeException {
    public OrderNotPendingException(String message) {
        super(message);
    }
}
