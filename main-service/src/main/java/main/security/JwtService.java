package main.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class JwtService {

    private final SecretKey key;

    private final long accessTtlMs;

    private final long refreshTtlMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-ttl-ms}") long accessTtlMs, @Value("${app.jwt.refresh-ttl-ms}") long refreshTtlMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    public String generateAccessToken(String username, List<String> groups) {
        return generate(username, groups, accessTtlMs, "access");
    }

    public String generateRefreshToken(String username, List<String> groups) {
        return generate(username, groups, refreshTtlMs, "refresh");
    }

    private String generate(String username, List<String> groups, long ttl, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claims(Map.of("groups", groups, "type", type))
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public String extractJti(String token) {
        return parse(token).getId();
    }

    public Date extractExpiration(String token) {
        return parse(token).getExpiration();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractGroups(String token) {
        Object groups = parse(token).get("groups");
        return groups instanceof List ? (List<String>) groups : List.of();
    }

    public long getAccessTtlMs() {
        return accessTtlMs;
    }

}