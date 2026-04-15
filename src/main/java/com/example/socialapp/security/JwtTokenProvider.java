package com.example.socialapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * JWT Token Provider.
 * Responsible for creating and validating JWT tokens.
 */
@Slf4j
@Component
@SuppressWarnings("deprecation")
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationInMs;

    /**
     * Generate JWT token from authentication.
     */
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication.getName(), jwtExpirationInMs);
    }

    /**
     * Generate JWT token from email.
     */
    public String generateAccessToken(String email) {
        return generateToken(email, jwtExpirationInMs);
    }

    /**
     * Generate refresh token.
     */
    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpirationInMs);
    }

    /**
     * Generate JWT token with custom expiration.
     */
    private String generateToken(String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, getSigningKey())
            .compact();
    }

    /**
     * Get email/subject from JWT token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(getSigningKey())
            .parseClaimsJws(token)
            .getBody();

        return claims.getSubject();
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get signing key for JWT operations.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get expiration time from token.
     */
    public Long getExpirationTime(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
            return claims.getExpiration().getTime();
        } catch (JwtException ex) {
            log.error("Error parsing JWT token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }
}
