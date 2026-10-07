package com.example.ticket.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<String> handleEventNotFound(ItemNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(e.getMessage());
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<String> handleSeatUnavailable(SeatUnavailableException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(e.getMessage());
    }

    @ExceptionHandler(SeatNotHeldByUserException.class)
    public ResponseEntity<String> handleSeatNotHeldByUser(SeatNotHeldByUserException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(e.getMessage());
    }
}
