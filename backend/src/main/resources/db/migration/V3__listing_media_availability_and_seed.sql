ALTER TABLE listings ADD COLUMN IF NOT EXISTS sample_listing boolean NOT NULL DEFAULT false;

CREATE TABLE listing_photos (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  uuid NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    url         text NOT NULL,
    position    integer NOT NULL DEFAULT 0,
    UNIQUE (listing_id, position)
);

CREATE INDEX listing_photos_listing_idx ON listing_photos (listing_id);

CREATE TABLE listing_availability (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  uuid NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time  time NOT NULL,
    end_time    time NOT NULL,
    CHECK (start_time < end_time)
);

CREATE INDEX listing_availability_listing_idx
    ON listing_availability (listing_id, day_of_week);

INSERT INTO users (email, password_hash, display_name)
VALUES (
    'host@parkhfx.example',
    '$2a$10$parkhfxSeedHostHashValueThatIsNotUsedForLogin',
    'ParkHFX Demo Host'
) ON CONFLICT DO NOTHING;

INSERT INTO listings (
    host_id, title, description, address, neighborhood, location,
    hourly_rate_cents, daily_max_cents, vehicle_size, covered, ev_charger,
    active, sample_listing
)
SELECT
    host.id,
    CASE (n % 5)
        WHEN 0 THEN 'Downtown Halifax Parking'
        WHEN 1 THEN 'Convenient Halifax Spot'
        WHEN 2 THEN 'Covered Parking Space'
        WHEN 3 THEN 'Secure ParkHFX Listing'
        ELSE 'Easy Halifax Parking'
    END || ' #' || (n + 1),
    'A sample parking space for the ParkHFX Halifax marketplace demo.',
    CASE (n % 5)
        WHEN 0 THEN 'Barrington Street'
        WHEN 1 THEN 'Spring Garden Road'
        WHEN 2 THEN 'Gottingen Street'
        WHEN 3 THEN 'Robie Street'
        ELSE 'Portland Street'
    END || ' ' || (100 + n),
    CASE
        WHEN n < 18 THEN 'Downtown Halifax'
        WHEN n < 32 THEN 'North End'
        WHEN n < 46 THEN 'South End'
        WHEN n < 58 THEN 'Dartmouth'
        ELSE 'Central Halifax'
    END,
    ST_SetSRID(ST_MakePoint(
        CASE
            WHEN n < 18 THEN -63.5780 - ((n % 6) * 0.0018)
            WHEN n < 32 THEN -63.5980 + ((n % 7) * 0.0015)
            WHEN n < 46 THEN -63.5740 - ((n % 7) * 0.0014)
            WHEN n < 58 THEN -63.5720 + ((n % 6) * 0.0022)
            ELSE -63.5880 + ((n % 6) * 0.0018)
        END,
        CASE
            WHEN n < 18 THEN 44.6465 + ((n % 5) * 0.0016)
            WHEN n < 32 THEN 44.6610 - ((n % 6) * 0.0012)
            WHEN n < 46 THEN 44.6355 + ((n % 7) * 0.0012)
            WHEN n < 58 THEN 44.6700 - ((n % 6) * 0.0017)
            ELSE 44.6400 + ((n % 7) * 0.0015)
        END
    ), 4326)::geography,
    300 + ((n % 8) * 100),
    1800 + ((n % 5) * 400),
    CASE (n % 3)
        WHEN 0 THEN 'COMPACT'
        WHEN 1 THEN 'STANDARD'
        ELSE 'LARGE'
    END,
    (n % 4 = 0),
    (n % 7 = 0),
    true,
    true
FROM generate_series(0, 69) AS s(n)
CROSS JOIN users host
WHERE host.email = 'host@parkhfx.example'
ON CONFLICT DO NOTHING;

WITH listed AS (
    SELECT id, n
    FROM (
        SELECT id, row_number() OVER (ORDER BY created_at, id) - 1 AS n
        FROM listings
        WHERE sample_listing = true
    ) ranked
)
INSERT INTO listing_photos (listing_id, url, position)
SELECT
    id,
    'https://images.unsplash.com/photo-1590674899484-d5640e854abe'
        || '?auto=format&fit=crop&w=900&q=80',
    0
FROM listed
ON CONFLICT DO NOTHING;

INSERT INTO listing_availability (listing_id, day_of_week, start_time, end_time)
SELECT id, day_of_week, '06:00'::time, '23:59'::time
FROM listings
CROSS JOIN generate_series(0, 6) AS day_of_week
WHERE sample_listing = true
ON CONFLICT DO NOTHING;
