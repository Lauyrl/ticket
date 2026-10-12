-- Migration: Create orders and order_items tables

CREATE TABLE orders (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code    VARCHAR(50)    NOT NULL UNIQUE,
    user_id       BIGINT         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    event_id      BIGINT         NOT NULL REFERENCES events(id) ON DELETE RESTRICT,
    hold_id       BIGINT         NOT NULL REFERENCES seat_holds(id) ON DELETE RESTRICT,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED')),
    total_amount  DECIMAL(15,2)  NOT NULL CHECK (total_amount >= 0),
    expires_at    TIMESTAMPTZ    NOT NULL,
    paid_at       TIMESTAMPTZ    NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_orders_updated BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_orders_user ON orders (user_id, status);
CREATE INDEX idx_orders_event ON orders (event_id, status);
CREATE INDEX idx_orders_hold ON orders (hold_id);

CREATE TABLE order_items (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    seat_id     BIGINT        NOT NULL REFERENCES event_seats(id) ON DELETE RESTRICT,
    zone_name   VARCHAR(100)  NOT NULL,
    row_label   VARCHAR(10)   NOT NULL,
    seat_number INT           NOT NULL,
    unit_price  DECIMAL(15,2) NOT NULL CHECK (unit_price >= 0),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_seat ON order_items (seat_id);
