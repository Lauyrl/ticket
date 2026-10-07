
INSERT INTO seat_zones (event_id, name, price)
VALUES
(1, 'VIP', 500000),
(1, 'Premium', 300000),
(1, 'Standard', 150000);

INSERT INTO event_seats (event_id, zone_id, row_index, seat_number)
SELECT 1, z.id, r.row_index, s.seat_number
FROM seat_zones z
CROSS JOIN LATERAL generate_series(
    CASE z.name
        WHEN 'VIP' THEN 1
        WHEN 'Premium' THEN 4
        WHEN 'Standard' THEN 9
    END,
    CASE z.name
        WHEN 'VIP' THEN 3
        WHEN 'Premium' THEN 8
        WHEN 'Standard' THEN 15
    END
) AS r(row_index)
CROSS JOIN LATERAL generate_series(
    1,
    CASE z.name
        WHEN 'VIP' THEN 10
        WHEN 'Premium' THEN 12
        WHEN 'Standard' THEN 15
    END
) AS s(seat_number)
WHERE z.event_id = 1;
