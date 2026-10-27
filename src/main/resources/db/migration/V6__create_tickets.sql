
-- Migration: Create tickets table

CREATE TABLE tickets (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_code   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    order_item_id BIGINT NOT NULL UNIQUE REFERENCES order_items(id) ON DELETE RESTRICT,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    event_id      BIGINT NOT NULL REFERENCES events(id) ON DELETE RESTRICT,
    seat_id       BIGINT NOT NULL REFERENCES event_seats(id) ON DELETE RESTRICT,
    status        VARCHAR(20) NOT NULL DEFAULT 'VALID'
                  CHECK (status IN ('VALID', 'USED', 'CANCELLED')),
    issued_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at       TIMESTAMPTZ
);

CREATE INDEX idx_tickets_user ON tickets (user_id, status);
CREATE INDEX idx_tickets_event ON tickets (event_id, status);
CREATE INDEX idx_tickets_seat ON tickets (seat_id);
