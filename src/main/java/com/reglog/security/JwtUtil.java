package com.reglog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for creating and validating JWTs using the JJWT library.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMillis) {
        // HS256 requires a key of at least 256 bits (32+ bytes).
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * Generates a signed JWT containing the user ID and username.
     * The token is not stored by the client in localStorage - it is sent
     * to the browser inside an HttpOnly cookie by the AuthService.
     */
    public String generateToken(Long userId, String username) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(username)             // "sub" claim = username
                .claim("userId", userId)       // custom claim = user ID
                .issuedAt(issuedAt)            // "iat"
                .expiration(expiresAt)         // "exp"
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token signature and returns its claims.
     * Throws JwtException if the signature is wrong or the token is expired.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}