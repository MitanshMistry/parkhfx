package com.parkhfx.listing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.parkhfx.listing.ListingDtos.AvailabilityWindow;
import com.parkhfx.listing.ListingDtos.ListingDetail;
import com.parkhfx.listing.ListingDtos.ListingSummary;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ListingRepository {

    private final JdbcTemplate jdbc;

    public ListingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ListingSummary> findInBounds(
            double minLongitude, double minLatitude,
            double maxLongitude, double maxLatitude) {
        if (minLongitude >= maxLongitude || minLatitude >= maxLatitude) {
            throw new IllegalArgumentException("Map bounds are invalid.");
        }
        return jdbc.query("""
                SELECT l.id, l.title, l.address, l.neighborhood,
                       l.hourly_rate_cents, l.daily_max_cents, l.vehicle_size,
                       l.covered, l.ev_charger,
                       ST_Y(l.location::geometry) AS latitude,
                       ST_X(l.location::geometry) AS longitude,
                       photo.url AS photo_url
                  FROM listings l
                  LEFT JOIN LATERAL (
                      SELECT url
                        FROM listing_photos
                       WHERE listing_id = l.id
                       ORDER BY position
                       LIMIT 1
                  ) photo ON true
                 WHERE l.active = true
                   AND l.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)::geography
                 ORDER BY l.hourly_rate_cents, l.title
                 LIMIT 200
                """, SUMMARY_MAPPER,
                minLongitude, minLatitude, maxLongitude, maxLatitude);
    }

    public List<ListingSummary> findNear(double longitude, double latitude, double radiusMeters) {
        if (radiusMeters <= 0 || radiusMeters > 100_000) {
            throw new IllegalArgumentException("Radius must be greater than zero and at most 100 km.");
        }
        return jdbc.query("""
                SELECT l.id, l.title, l.address, l.neighborhood,
                       l.hourly_rate_cents, l.daily_max_cents, l.vehicle_size,
                       l.covered, l.ev_charger,
                       ST_Y(l.location::geometry) AS latitude,
                       ST_X(l.location::geometry) AS longitude,
                       photo.url AS photo_url
                  FROM listings l
                  LEFT JOIN LATERAL (
                      SELECT url
                        FROM listing_photos
                       WHERE listing_id = l.id
                       ORDER BY position
                       LIMIT 1
                  ) photo ON true
                 WHERE l.active = true
                   AND ST_DWithin(
                       l.location,
                       ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                       ?
                   )
                 ORDER BY ST_Distance(
                     l.location,
                     ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                 ), l.title
                 LIMIT 200
                """, SUMMARY_MAPPER, longitude, latitude, radiusMeters, longitude, latitude);
    }

    public List<ListingSummary> findAll() {
        return jdbc.query("""
                SELECT l.id, l.title, l.address, l.neighborhood,
                       l.hourly_rate_cents, l.daily_max_cents, l.vehicle_size,
                       l.covered, l.ev_charger,
                       ST_Y(l.location::geometry) AS latitude,
                       ST_X(l.location::geometry) AS longitude,
                       photo.url AS photo_url
                  FROM listings l
                  LEFT JOIN LATERAL (
                      SELECT url
                        FROM listing_photos
                       WHERE listing_id = l.id
                       ORDER BY position
                       LIMIT 1
                  ) photo ON true
                 WHERE l.active = true
                 ORDER BY l.hourly_rate_cents, l.title
                 LIMIT 200
                """, SUMMARY_MAPPER);
    }

    public Optional<ListingDetail> findDetail(UUID id) {
        List<ListingDetail> matches = jdbc.query("""
                SELECT l.id, l.title, l.description, l.address, l.neighborhood,
                       l.hourly_rate_cents, l.daily_max_cents, l.vehicle_size,
                       l.covered, l.ev_charger,
                       ST_Y(l.location::geometry) AS latitude,
                       ST_X(l.location::geometry) AS longitude,
                       u.display_name AS host_name
                  FROM listings l
                  JOIN users u ON u.id = l.host_id
                 WHERE l.id = ?::uuid AND l.active = true
                """, (rs, rowNum) -> mapDetail(rs), id);

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        ListingDetail base = matches.getFirst();
        List<String> photos = jdbc.queryForList("""
                SELECT url
                  FROM listing_photos
                 WHERE listing_id = ?::uuid
                 ORDER BY position
                """, String.class, id);

        List<AvailabilityWindow> availability = jdbc.query("""
                SELECT day_of_week,
                       to_char(start_time, 'HH24:MI') AS start_time,
                       to_char(end_time, 'HH24:MI') AS end_time
                  FROM listing_availability
                 WHERE listing_id = ?::uuid
                 ORDER BY day_of_week
                """, (rs, rowNum) -> new AvailabilityWindow(
                        rs.getInt("day_of_week"),
                        rs.getString("start_time"),
                        rs.getString("end_time")),
                id);

        return Optional.of(new ListingDetail(
                base.id(), base.title(), base.description(), base.address(),
                base.neighborhood(), base.hourlyRateCents(), base.dailyMaxCents(),
                base.vehicleSize(), base.covered(), base.evCharger(),
                base.latitude(), base.longitude(), base.hostName(),
                photos, availability));
    }

    private static ListingDetail mapDetail(ResultSet rs) throws SQLException {
        return new ListingDetail(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("address"),
                rs.getString("neighborhood"),
                rs.getInt("hourly_rate_cents"),
                rs.getInt("daily_max_cents"),
                rs.getString("vehicle_size"),
                rs.getBoolean("covered"),
                rs.getBoolean("ev_charger"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getString("host_name"),
                List.of(),
                List.of());
    }

    private static final RowMapper<ListingSummary> SUMMARY_MAPPER =
            (rs, rowNum) -> new ListingSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("address"),
                    rs.getString("neighborhood"),
                    rs.getInt("hourly_rate_cents"),
                    rs.getInt("daily_max_cents"),
                    rs.getString("vehicle_size"),
                    rs.getBoolean("covered"),
                    rs.getBoolean("ev_charger"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getString("photo_url"));
}
