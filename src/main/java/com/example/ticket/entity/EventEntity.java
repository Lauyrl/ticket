package com.example.ticket.entity;

import java.time.Instant;

import com.example.ticket.enums.EventStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter 
@Setter 
@NoArgsConstructor 
public class EventEntity {
    /*
    @Column()
        name: Exact column name in DB
        length: Maximum size of a string column (default is 255)
        nullable: Whether the column can contain NULL values (default is true)
        unique: Ensures that all values in this column are unique
        precision: Defines the total number of digits for numeric columns
        scale: Defines the number of digits to the right of the decimal point
        insertable: Determines whether the column is included in SQL INSERT statements
        updatable: Determines whether the column is included in SQL UPDATE statements
    */
    /*  
    CREATE TABLE events (
        id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name                  VARCHAR(500) NOT NULL,
        description           TEXT,
        category              VARCHAR(100),
        venue                 VARCHAR(500) NOT NULL,
        city                  VARCHAR(255),
        event_date            TIMESTAMPTZ  NOT NULL,
        starts_at             TIMESTAMPTZ NOT NULL,
        ends_at               TIMESTAMPTZ,
        image_url             VARCHAR(1000),
        status                VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING' CHECK (status IN ('UPCOMING', 'ON_SALE', 'ENDED', 'CANCELLED')),
        max_seats_per_user    INT          NOT NULL DEFAULT 2  CHECK (max_seats_per_user > 0),
        hold_duration_minutes INT          NOT NULL DEFAULT 10 CHECK (hold_duration_minutes > 0),
        created_by            BIGINT       NOT NULL, -- REFERENCES users(id) ON DELETE SET NULL,
        created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
        updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
    );
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String name;

    @Column
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 500, nullable = false)
    private String venue;

    @Column(length = 255)
    private String city;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EventStatus status;

    @Column(name = "max_seats_per_user", nullable = false)
    private int maxSeatsPerUser;

    @Column(name = "hold_duration_minutes", nullable = false)
    private int holdDurationMinutes;

    @Column(name = "created_by", nullable = false, insertable = false)
    private long createdBy;

    @Column(name = "created_at", nullable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
