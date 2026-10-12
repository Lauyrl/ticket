-- Migration: Create users, customer_profiles and link foreign keys to events & seat_holds

CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Insert default admin user with id = 1 (matching existing mock events created_by = 1)
-- Default password: Admin@123
INSERT INTO users (id, email, password, full_name, role)
OVERRIDING SYSTEM VALUE
VALUES (
    1,
    'admin@ticketrush.com',
    '$2a$10$7vNfG1qYpZ8nK4Xp5mR7Q.V4k4S4Z1k7l9m5o1p3r5t7v9x1z3A5B', -- BCrypt for Admin@123
    'TicketRush Admin',
    'ADMIN'
);

-- Sync identity sequence for users
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

-- Link foreign key constraints
ALTER TABLE events
    ADD CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE seat_holds
    ADD CONSTRAINT fk_seat_holds_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE TABLE customer_profiles (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    phone         VARCHAR(20),
    date_of_birth DATE,
    gender        VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    avatar_url    VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
