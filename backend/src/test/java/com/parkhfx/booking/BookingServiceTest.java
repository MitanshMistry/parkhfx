package com.parkhfx.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs against a real PostgreSQL + PostGIS database in Docker (Testcontainers),
 * because the thing under test is a database constraint.
 */
@SpringBootTest
@Testcontainers
class BookingServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    BookingService bookings;

    @Autowired
    JdbcTemplate jdbc;

    UUID hostId;
    UUID driverId;
    UUID listingId;

    // Two days from now, on the hour, so nothing here is in the past.
    final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC)
            .plusDays(2).truncatedTo(ChronoUnit.HOURS);

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE bookings, listings, users CASCADE");
        hostId = insertUser("host@example.com");
        driverId = insertUser("driver@example.com");
        listingId = insertListing(hostId, -63.5752, 44.6488); // downtown Halifax
    }

    @Test
    void overlappingHoldIsRejected() {
        bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);

        assertThatThrownBy(() ->
                bookings.createHold(listingId, driverId, base.plusHours(10), base.plusHours(12), 1000))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void backToBackBookingsAreAllowed() {
        bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);

        // Starts exactly when the first one ends: [9,11) and [11,13) do not overlap.
        UUID second = bookings.createHold(listingId, driverId, base.plusHours(11), base.plusHours(13), 1000);

        assertThat(second).isNotNull();
    }

    @Test
    void differentListingsDoNotConflict() {
        UUID otherListing = insertListing(hostId, -63.6000, 44.6600);
        bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);

        UUID other = bookings.createHold(otherListing, driverId, base.plusHours(9), base.plusHours(11), 1000);

        assertThat(other).isNotNull();
    }

    @Test
    void cancellingFreesTheSlot() {
        UUID first = bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);

        assertThat(bookings.cancel(first)).isTrue();

        UUID again = bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);
        assertThat(again).isNotNull();
    }

    @Test
    void expiredHoldDoesNotBlockTheSlot() {
        // An abandoned hold: still PENDING, but its 10 minutes ran out a minute ago.
        UUID stale = insertRawPendingHold(base.plusHours(9), base.plusHours(11),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        UUID fresh = bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);

        assertThat(fresh).isNotNull();
        assertThat(statusOf(stale)).isEqualTo("EXPIRED");
        assertThat(statusOf(fresh)).isEqualTo("PENDING");
    }

    @Test
    void onlyUnexpiredHoldsCanBeConfirmed() {
        UUID live = bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);
        UUID stale = insertRawPendingHold(base.plusHours(14), base.plusHours(15),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        assertThat(bookings.confirm(live)).isTrue();
        assertThat(statusOf(live)).isEqualTo("CONFIRMED");
        assertThat(bookings.confirm(stale)).isFalse();
    }

    @Test
    void invalidTimeRangeIsRejected() {
        assertThatThrownBy(() ->
                bookings.createHold(listingId, driverId, base.plusHours(11), base.plusHours(9), 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyOneOfManyConcurrentRequestsWins() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await(); // release every thread at the same moment
                try {
                    bookings.createHold(listingId, driverId, base.plusHours(9), base.plusHours(11), 1000);
                    return true;
                } catch (BookingConflictException e) {
                    return false;
                }
            }));
        }

        ready.await();
        go.countDown();

        long winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        pool.shutdown();

        assertThat(winners).isEqualTo(1);
        Integer active = jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE listing_id = ?::uuid AND status IN ('PENDING','CONFIRMED')",
                Integer.class, listingId);
        assertThat(active).isEqualTo(1);
    }

    @Test
    void postgisFindsNearbyListingsOnly() {
        // Search 2 km around a downtown Halifax point.
        String sql = """
                SELECT count(*) FROM listings
                 WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, 2000)
                """;

        Integer nearHalifax = jdbc.queryForObject(sql, Integer.class, -63.5700, 44.6500);
        Integer nearToronto = jdbc.queryForObject(sql, Integer.class, -79.3800, 43.6500);

        assertThat(nearHalifax).isEqualTo(1);
        assertThat(nearToronto).isEqualTo(0);
    }

    // ---- helpers -----------------------------------------------------------

    private UUID insertUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password_hash, display_name)
                VALUES (?, 'not-a-real-hash', 'Test User')
                RETURNING id
                """, UUID.class, email);
    }

    private UUID insertListing(UUID host, double lon, double lat) {
        return jdbc.queryForObject("""
                INSERT INTO listings (host_id, title, address, neighborhood, location,
                                      hourly_rate_cents, daily_max_cents)
                VALUES (?::uuid, 'Test spot', '1 Test St', 'Downtown',
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, 500, 2500)
                RETURNING id
                """, UUID.class, host, lon, lat);
    }

    /** Inserts a PENDING hold directly, bypassing the service, with a chosen expiry. */
    private UUID insertRawPendingHold(OffsetDateTime start, OffsetDateTime end, OffsetDateTime expiresAt) {
        return jdbc.queryForObject("""
                INSERT INTO bookings (listing_id, driver_id, during, status, expires_at, total_cents)
                VALUES (?::uuid, ?::uuid, tstzrange(?::timestamptz, ?::timestamptz, '[)'),
                        'PENDING', ?::timestamptz, 1000)
                RETURNING id
                """, UUID.class, listingId, driverId, start, end, expiresAt);
    }

    private String statusOf(UUID bookingId) {
        return jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?::uuid", String.class, bookingId);
    }
}
