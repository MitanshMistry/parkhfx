-- V1: core schema. Flyway migrations are immutable once merged; change things
-- with a new V2__, V3__ ... file instead of editing this one.

CREATE EXTENSION IF NOT EXISTS postgis;     -- geography type + spatial index
CREATE EXTENSION IF NOT EXISTS btree_gist;  -- lets a GiST exclusion constraint compare uuid with "="

-- ---------------------------------------------------------------------------
-- Users: one account type. Anyone can host and drive.
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text        NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    display_name  text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Listings: a parking spot offered by a host. Money is integer cents (CAD).
-- ---------------------------------------------------------------------------
CREATE TABLE listings (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id           uuid        NOT NULL REFERENCES users (id),
    title             text        NOT NULL,
    description       text        NOT NULL DEFAULT '',
    address           text        NOT NULL,
    neighborhood      text        NOT NULL,
    location          geography(Point, 4326) NOT NULL,
    hourly_rate_cents integer     NOT NULL CHECK (hourly_rate_cents >= 0),
    daily_max_cents   integer     NOT NULL CHECK (daily_max_cents >= 0),
    vehicle_size      text        NOT NULL DEFAULT 'STANDARD'
                                  CHECK (vehicle_size IN ('COMPACT', 'STANDARD', 'LARGE')),
    covered           boolean     NOT NULL DEFAULT false,
    ev_charger        boolean     NOT NULL DEFAULT false,
    active            boolean     NOT NULL DEFAULT true,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX listings_location_gix ON listings USING gist (location);
CREATE INDEX listings_host_idx     ON listings (host_id);

-- ---------------------------------------------------------------------------
-- Bookings. "during" is a half-open range [start, end) in UTC, so a booking
-- ending at 11:00 and another starting at 11:00 do not collide.
-- ---------------------------------------------------------------------------
CREATE TABLE bookings (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  uuid        NOT NULL REFERENCES listings (id),
    driver_id   uuid        NOT NULL REFERENCES users (id),
    during      tstzrange   NOT NULL,
    status      text        NOT NULL
                            CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    expires_at  timestamptz,
    total_cents integer     NOT NULL CHECK (total_cents >= 0),
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT bookings_range_valid
        CHECK (NOT isempty(during) AND NOT lower_inf(during) AND NOT upper_inf(during)),

    CONSTRAINT bookings_pending_has_expiry
        CHECK (status <> 'PENDING' OR expires_at IS NOT NULL),

    -- The important one: no two active bookings/holds may overlap on one listing.
    CONSTRAINT bookings_no_overlap
        EXCLUDE USING gist (listing_id WITH =, during WITH &&)
        WHERE (status IN ('PENDING', 'CONFIRMED'))
);

CREATE INDEX bookings_driver_idx ON bookings (driver_id);

-- ---------------------------------------------------------------------------
-- A constraint cannot look at now(), so abandoned holds are released by this
-- function. The app calls it inside the same transaction right before inserting
-- a new hold. Returns how many holds were expired.
-- ---------------------------------------------------------------------------
CREATE FUNCTION expire_stale_holds(p_listing_id uuid, p_as_of timestamptz)
RETURNS integer
LANGUAGE sql
AS $$
    WITH expired AS (
        UPDATE bookings
           SET status = 'EXPIRED'
         WHERE listing_id = p_listing_id
           AND status = 'PENDING'
           AND expires_at <= p_as_of
        RETURNING 1
    )
    SELECT count(*)::integer FROM expired;
$$;
