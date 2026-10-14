package com.checkautos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService();
        ReflectionTestUtils.setField(jwtTokenService, "jwtSecret", "checkautos-super-secret-key-2024-must-be-at-least-256bits-long!!");
        ReflectionTestUtils.setField(jwtTokenService, "jwtExpirationMs", 3600000L); // 1 hora
    }

    @Test
    @DisplayName("Debe generar un token válido y extraer claims de email y rol")
    void testGenerateAndParseToken() {
        String token = jwtTokenService.generateToken("test@checkautos.com", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtTokenService.validateToken(token));
        assertEquals("test@checkautos.com", jwtTokenService.extractUsername(token));
        assertEquals("ADMIN", jwtTokenService.extractRol(token));
        assertNotNull(jwtTokenService.getExpirationDate(token));
    }

    @Test
    @DisplayName("Debe rechazar un token manipulado o inválido")
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidpayload.signature";
        assertFalse(jwtTokenService.validateToken(invalidToken));
        assertFalse(jwtTokenService.validateToken(""));
        assertFalse(jwtTokenService.validateToken(null));
    }
}
