package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(Mockito.mock(com.project.back_end.repo.AdminRepository.class),
                Mockito.mock(com.project.back_end.repo.DoctorRepository.class),
                Mockito.mock(com.project.back_end.repo.PatientRepository.class));
        ReflectionTestUtils.setField(tokenService, "secret",
                "$!@#$^%$$$%####$DDCPN0234FCFDPD8670M");
    }

    @Test
    void generateAndExtractRoleAndIdentifier() throws Exception {
        String token = tokenService.generateToken("dr.adams@example.com", "doctor");
        assertEquals("doctor", tokenService.extractRole(token));
        assertEquals("dr.adams@example.com", tokenService.extractIdentifier(token));
    }

    @Test
    void tamperedTokenFailsExtraction() {
        String token = tokenService.generateToken("admin", "admin");
        assertThrows(Exception.class, () -> tokenService.extractRole(token + "x"));
    }
}
