package com.spring.dubbyserver.global.security;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration ttl;

    public JwtProvider(DubbyProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.auth().jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.ttl = properties.auth().accessTokenTtl();
    }

    public String issue(UUID userId, String deviceId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("did", deviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    /** @throws DubbyException AUTH_TOKEN_EXPIRED | AUTH_TOKEN_INVALID */
    public UUID parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new DubbyException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new DubbyException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }
}
