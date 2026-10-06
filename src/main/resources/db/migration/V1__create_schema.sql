-- (PostgreSQL 13+)
-- enums = VARCHAR + CHECK
-- timestamps = TIMESTAMPTZ
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger 
AS ' 
BEGIN
    NEW.updated_at := now();    -- NEW: pending updated version of the row that triggers the function 
    RETURN NEW;
END; 
' LANGUAGE plpgsql;

--------- uncomment to reset
-- DROP TABLE IF EXISTS notifications, queue_sessions, tickets, order_items, orders,
-- seat_holds, event_seats, seat_zones, events,
-- password_reset_otps, customer_profiles, users CASCADE;

CREATE TABLE events (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                  VARCHAR(500) NOT NULL,
    description           TEXT,
    category              VARCHAR(100),
    venue                 VARCHAR(500) NOT NULL,
    city                  VARCHAR(255),
    starts_at             TIMESTAMPTZ NOT NULL,
    ends_at               TIMESTAMPTZ NOT NULL,
    image_url             VARCHAR(1000),
    status                VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING' CHECK (status IN ('UPCOMING', 'ON_SALE', 'ENDED', 'CANCELLED')),
    max_seats_per_user    INT          NOT NULL DEFAULT 2  CHECK (max_seats_per_user > 0),
    hold_duration_minutes INT          NOT NULL DEFAULT 10 CHECK (hold_duration_minutes > 0),
    created_by            BIGINT       NOT NULL, -- REFERENCES users(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_events_updated BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE seat_zones (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id       BIGINT        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name           VARCHAR(100)  NOT NULL,
    price          DECIMAL(15,2) NOT NULL CHECK (price > 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (event_id, name),
    UNIQUE (id, event_id)       -- target for the composite FK in event_seats
);

-- pure geometry: availability is derived from seat_holds, never stored here
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
CREATE INDEX idx_event_seats_zone ON event_seats (zone_id, row_index, seat_number); -- WHERE zone_id=? AND row_index=? seat_number=?

-- one row per held seat
-- ACTIVE    = temporarily held (valid only while expires_at > now())
-- CONVERTED = paid, seat is sold
-- EXPIRED / RELEASED = seat is free again (refund/cancel -> RELEASED)
CREATE TABLE seat_holds (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL, -- REFERENCES users(id) ON DELETE SET NULL,
    seat_id     BIGINT      NOT NULL REFERENCES event_seats(id) ON DELETE SET NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'RELEASED', 'CONVERTED')),
    held_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    CHECK (expires_at > held_at)
);
CREATE UNIQUE INDEX ux_seat_holds_taken_seat ON seat_holds (seat_id) WHERE status IN ('ACTIVE', 'CONVERTED'); -- a new hold cant be inserted if a hold is already active, or if a hold converted to sale
CREATE INDEX idx_seat_holds_user   ON seat_holds (user_id, status);                                           -- WHERE user_id=? AND status='ACTIVE' (seat cap, "my holds")
CREATE INDEX idx_seat_holds_expiry ON seat_holds (expires_at) WHERE status = 'ACTIVE';                        -- WHERE status='ACTIVE' AND expires_at < now()
CREATE INDEX idx_seat_holds_seat   ON seat_holds (seat_id);
