package com.example.ticket.dto.event;

import java.time.Instant;

import com.example.ticket.enums.EventStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
    @Size(max = 500) @NotBlank String name,
    String description,
    @Size(max = 100) String category,
    @Size(max = 500) @NotBlank String venue,
    @Size(max = 255) String city,
    Instant startsAt,
    Instant endsAt,
    @Size(max = 1000) String imageUrl,
    EventStatus status,
    @Positive Integer maxSeatsPerUser,
    @Positive Integer holdDurationMinutes
) {
    // only compares between timestamps provided for update, not existing fields (partial updates for only startsAt or only endsAt)
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isEndTimeAfterStartTime() {
        return startsAt == null || endsAt == null || !endsAt.isBefore(startsAt);
    }
}
