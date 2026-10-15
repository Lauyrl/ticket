package com.example.ticket.dto.event;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSeatZoneRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal price,
    @NotEmpty List<@Valid CreateSeatRequest> seats
) {}
