INSERT INTO events (
    name,
    description,
    category,
    venue,
    city,
    starts_at,
    ends_at,
    image_url,
    status,
    max_seats_per_user,
    hold_duration_minutes,
    created_by
)
VALUES
(
    'Rock Festival 2026',
    'A live rock music festival featuring local and international bands.',
    'Music',
    'My Dinh National Stadium',
    'Hanoi',
    '2026-11-15 19:00:00+07',
    '2026-11-15 23:00:00+07',
    1,
    'ON_SALE',
    4,
    10,
    1
),
(
    'Vietnam Tech Conference 2026',
    'A technology conference covering software engineering and AI.',
    'Technology',
    'National Convention Center',
    'Hanoi',
    '2026-12-05 09:00:00+07',
    '2026-12-05 17:00:00+07',
    1,
    'UPCOMING',
    2,
    10,
    1
),
(
    'Hanoi Jazz Night',
    'An evening of live jazz performances.',
    'Music',
    'Hanoi Opera House',
    'Hanoi',
    '2026-10-25 20:00:00+07',
    '2026-10-25 22:30:00+07',
    1,
    'ON_SALE',
    2,
    10,
    1
);
