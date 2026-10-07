package com.example.ticket.entity.event;

import java.math.BigDecimal;
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
@Table(name = "seat_zones")
@Getter 
@Setter 
@NoArgsConstructor 
public class SeatZoneEntity {
    /*
    CREATE TABLE seat_zones (
        id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        event_id       BIGINT        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
        name           VARCHAR(100)  NOT NULL,
        price          DECIMAL(15,2) NOT NULL CHECK (price > 0),
        created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
        UNIQUE (event_id, name),
        UNIQUE (id, event_id)       -- target for the composite FK in event_seats
    );
    */
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column (length = 100, nullable = false)
    private String name;

    @Column (precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Column (name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;


}
