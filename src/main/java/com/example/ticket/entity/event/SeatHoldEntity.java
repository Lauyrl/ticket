package com.example.ticket.entity.event;

import java.time.Instant;

import com.example.ticket.enums.SeatHoldStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "seat_holds")
@Getter 
@Setter 
@NoArgsConstructor 
public class SeatHoldEntity {
    /*
    CREATE TABLE seat_holds (
        id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        user_id     BIGINT      NOT NULL, -- REFERENCES users(id) ON DELETE SET NULL,
        seat_id     BIGINT      NOT NULL REFERENCES event_seats(id) ON DELETE SET NULL,
        status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'RELEASED', 'CONVERTED')),
        held_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
        expires_at  TIMESTAMPTZ NOT NULL,
        CHECK (expires_at > held_at)
    );
    CREATE UNIQUE INDEX ux_seat_holds_taken_seat ON seat_holds (seat_id) WHERE status IN ('ACTIVE', 'CONVERTED');
    */
    public SeatHoldEntity(SeatEntity seat, Instant expiresAt) { //, Long userId
        this.seat = seat;
        this.status = SeatHoldStatus.ACTIVE; // ACTIVE by default upon creation
        this.expiresAt = expiresAt;
    }

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: add when UserEntity exists
    // @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn(name = "user_id", nullable = true)
    // private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = true)
    private SeatEntity seat;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SeatHoldStatus status;

    @Column(name = "held_at", nullable = false, insertable = false, updatable = false)
    private Instant heldAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
