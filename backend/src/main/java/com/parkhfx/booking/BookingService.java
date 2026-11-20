package com.parkhfx.booking;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking lifecycle: PENDING (hold) -> CONFIRMED, or EXPIRED / CANCELLED.
 *
 * Double-booking is prevented by an exclusion constraint in the database
 * (see V1__init_schema.sql and docs/adr/0001), not by "check then insert" here.
 */
@Service
public class BookingService {

    /** How long a driver has to pay before the hold is released. */
    public static final Duration HOLD_TTL = Duration.ofMinutes(10);

    /** PostgreSQL SQLSTATE for exclusion_violation. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    private static final String INSERT_HOLD = """
            INSERT INTO bookings (listing_id, driver_id, during, status, expires_at, total_cents)
            VALUES (?::uuid, ?::uuid, tstzrange(?::timestamptz, ?::timestamptz, '[)'),
                    'PENDING', ?::timestamptz, ?)
            RETURNING id
            """;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public BookingService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Places a 10-minute hold on a spot for [start, end).
     *
     * @throws BookingConflictException if the time range overlaps an active booking or hold
     */
    @Transactional
    public UUID createHold(UUID listingId, UUID driverId,
                           OffsetDateTime start, OffsetDateTime end, int totalCents) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);

        // Release abandoned holds on this listing so they stop blocking the slot.
        jdbc.queryForObject("SELECT expire_stale_holds(?::uuid, ?::timestamptz)",
                Integer.class, listingId, now);

        try {
            return jdbc.queryForObject(INSERT_HOLD, UUID.class,
                    listingId, driverId, start, end, now.plus(HOLD_TTL), totalCents);
        } catch (DataAccessException e) {
            if (isExclusionViolation(e)) {
                throw new BookingConflictException();
            }
            throw e;
        }
    }

    /** Confirms a hold (called after payment succeeds). Returns false if it expired or is not pending. */
    @Transactional
    public boolean confirm(UUID bookingId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        int updated = jdbc.update("""
                UPDATE bookings
                   SET status = 'CONFIRMED', expires_at = NULL
                 WHERE id = ?::uuid
                   AND status = 'PENDING'
                   AND expires_at > ?::timestamptz
                """, bookingId, now);
        return updated == 1;
    }

    /** Cancels a pending or confirmed booking, freeing the slot. Returns false if nothing changed. */
    @Transactional
    public boolean cancel(UUID bookingId) {
        int updated = jdbc.update("""
                UPDATE bookings
                   SET status = 'CANCELLED'
                 WHERE id = ?::uuid
                   AND status IN ('PENDING', 'CONFIRMED')
                """, bookingId);
        return updated == 1;
    }

    private static boolean isExclusionViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }
}
