package com.parkhfx.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.parkhfx.user.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived signed access tokens. The subject is the user id. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, Clock clock,
                        @Value("${app.jwt.ttl-minutes:1440}") long ttlMinutes) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("parkhfx")
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("email", user.email())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
