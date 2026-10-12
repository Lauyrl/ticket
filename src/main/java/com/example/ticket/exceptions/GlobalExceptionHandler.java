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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(e.getMessage());
    }

    @ExceptionHandler(MaxSeatsExceededException.class)
    public ResponseEntity<String> handleMaxSeatsExceeded(MaxSeatsExceededException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(e.getMessage());
    }

    @ExceptionHandler(HoldExpiredException.class)
    public ResponseEntity<String> handleHoldExpired(HoldExpiredException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(e.getMessage());
    }

    @ExceptionHandler(OrderAlreadyExistsException.class)
    public ResponseEntity<String> handleOrderAlreadyExists(OrderAlreadyExistsException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(e.getMessage());
    }

    @ExceptionHandler(OrderNotPendingException.class)
    public ResponseEntity<String> handleOrderNotPending(OrderNotPendingException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(e.getMessage());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException e) {
        String message = (e.getMessage() != null && !e.getMessage().isBlank() && !"Bad credentials".equalsIgnoreCase(e.getMessage()))
                ? e.getMessage()
                : "Invalid email or password";
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(message);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorMessage);
    }
}
