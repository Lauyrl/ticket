package com.example.ticket.dto.event;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateSeatRequest(
    @NotNull @PositiveOrZero int rowIndex,
    @NotNull @Positive int seatNumber
) {}
