package com.example.ticket.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.ticket.enums.UserRole;

class AuthSecurityTests {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        long accessExpirationMs = 1800000; // 30 mins
        long refreshExpirationMs = 604800000; // 7 days
        jwtTokenProvider = new JwtTokenProvider(secret, accessExpirationMs, refreshExpirationMs);
    }

    @Test
    void testBuildUserPrincipalDirectlyFromAccessTokenClaims() {
        UserPrincipal original = UserPrincipal.createFromClaims(100L, "test@example.com", "Test User", UserRole.CUSTOMER);
        String accessToken = jwtTokenProvider.generateAccessToken(original);

        assertTrue(jwtTokenProvider.validateAccessToken(accessToken));
        assertFalse(jwtTokenProvider.validateRefreshToken(accessToken));

        UserPrincipal principalFromToken = jwtTokenProvider.getUserPrincipalFromToken(accessToken);
        assertNotNull(principalFromToken);
        assertEquals(100L, principalFromToken.getId());
        assertEquals("test@example.com", principalFromToken.getEmail());
        assertEquals("Test User", principalFromToken.getFullName());
        assertEquals(UserRole.CUSTOMER, principalFromToken.getRole());
        assertNull(principalFromToken.getPassword());
        assertEquals(1, principalFromToken.getAuthorities().size());
        assertEquals("ROLE_CUSTOMER", principalFromToken.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testRefreshTokenValidation() {
        UserPrincipal principal = UserPrincipal.createFromClaims(200L, "admin@example.com", "Admin User", UserRole.ADMIN);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.validateAccessToken(refreshToken));
        assertEquals(200L, jwtTokenProvider.getUserIdFromToken(refreshToken));
    }
}
