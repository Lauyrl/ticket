package com.example.ticket.dto.event;

import java.time.Instant;
import java.util.List;

import com.example.ticket.enums.EventStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
    @NotBlank @Size(max = 500) String name,
    String description,
    @Size(max = 100) String category,
    @NotBlank @Size(max = 500) String venue,
    @Size(max = 255) String city,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 1000) String imageUrl,
    @NotNull EventStatus status,
    @NotNull @Positive Integer maxSeatsPerUser,
    @NotNull @Positive Integer holdDurationMinutes,
    List<@Valid CreateSeatZoneRequest> zones
) {
    @AssertTrue (message = "endsAt must be after startsAt")
    public boolean isEndTimeAfterStartTime() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
