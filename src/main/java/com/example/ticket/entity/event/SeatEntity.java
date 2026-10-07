package com.example.ticket.entity.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "event_seats")
@Getter
@Setter 
@NoArgsConstructor 
public class SeatEntity {
    /*
    CREATE TABLE event_seats (
        id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        event_id     BIGINT      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
        zone_id      BIGINT      NOT NULL,
        row_index    INT         NOT NULL,
        seat_number  INT         NOT NULL CHECK (seat_number > 0),  -- cycles per row
        created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
        UNIQUE (event_id, row_index, seat_number),
        FOREIGN KEY (zone_id, event_id) REFERENCES seat_zones(id, event_id)   -- zone must belong to the same event
    );
    */

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private SeatZoneEntity zone;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
