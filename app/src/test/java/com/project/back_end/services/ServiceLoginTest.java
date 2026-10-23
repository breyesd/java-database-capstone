package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.util.Map;

class ServiceLoginTest {

    private AuthenticationManager authenticationManager;
    private TokenService tokenService;
    private Service service;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        tokenService = Mockito.mock(TokenService.class);
        AdminRepository adminRepository = Mockito.mock(AdminRepository.class);
        DoctorRepository doctorRepository = Mockito.mock(DoctorRepository.class);
        PatientRepository patientRepository = Mockito.mock(PatientRepository.class);
        DoctorService doctorService = Mockito.mock(DoctorService.class);
        PatientService patientService = Mockito.mock(PatientService.class);
        service = new Service(tokenService, adminRepository, doctorRepository,
                patientRepository, doctorService, patientService, authenticationManager);
    }

    @Test
    void adminLoginSuccessReturnsToken() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("admin@1234");
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("admin", "admin@1234"))).thenReturn(auth);
        Mockito.when(tokenService.generateToken("admin", "admin")).thenReturn("jwt.admin");

        ResponseEntity<Map<String, String>> res = service.validateAdmin(admin);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("jwt.admin", res.getBody().get("token"));
    }

    @Test
    void adminLoginBadCredentialsReturns401() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("wrong");
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("bad"));

        ResponseEntity<Map<String, String>> res = service.validateAdmin(admin);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("Invalid username or password", res.getBody().get("message"));
    }

    @Test
    void patientLoginBadCredentialsReturns401WithPatientMessage() {
        Login login = new Login();
        login.setEmail("p@example.com");
        login.setPassword("wrong");
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("bad"));

        ResponseEntity<Map<String, String>> res = service.validatePatientLogin(login);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("Invalid email or password", res.getBody().get("message"));
    }
}
