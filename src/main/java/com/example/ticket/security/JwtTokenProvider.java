package com.example.ticket.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.ticket.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration-ms:${jwt.expiration-ms:1800000}}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .claim("email", userPrincipal.getEmail())
                .claim("fullName", userPrincipal.getFullName())
                .claim("role", userPrincipal.getRole().name())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .claim("email", userPrincipal.getEmail())
                .claim("fullName", userPrincipal.getFullName())
                .claim("role", userPrincipal.getRole().name())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateToken(UserPrincipal userPrincipal) {
        return generateAccessToken(userPrincipal);
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Long id = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String fullName = claims.get("fullName", String.class);
        String roleStr = claims.get("role", String.class);
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.CUSTOMER;
        return UserPrincipal.createFromClaims(id, email, fullName, role);
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType != null && !"ACCESS".equalsIgnoreCase(tokenType)) {
                log.warn("Token is not an access token: {}", tokenType);
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT access token: {}", ex.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return "REFRESH".equalsIgnoreCase(tokenType);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT refresh token: {}", ex.getMessage());
        }
        return false;
    }

    public boolean validateToken(String token) {
        return validateAccessToken(token);
    }
}
