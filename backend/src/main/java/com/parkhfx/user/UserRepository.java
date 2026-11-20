package com.parkhfx.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> MAPPER = (rs, row) -> new User(
            rs.getObject("id", UUID.class),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("display_name"));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Emails are stored lowercased, so callers pass an already-normalized email. */
    public Optional<User> findByEmail(String email) {
        return jdbc.query("""
                SELECT id, email, password_hash, display_name
                  FROM users
                 WHERE email = ?
                """, MAPPER, email).stream().findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbc.query("""
                SELECT id, email, password_hash, display_name
                  FROM users
                 WHERE id = ?::uuid
                """, MAPPER, id).stream().findFirst();
    }

    /** @throws org.springframework.dao.DuplicateKeyException if the email is already taken */
    public User insert(String email, String passwordHash, String displayName) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password_hash, display_name)
                VALUES (?, ?, ?)
                RETURNING id, email, password_hash, display_name
                """, MAPPER, email, passwordHash, displayName);
    }
}
