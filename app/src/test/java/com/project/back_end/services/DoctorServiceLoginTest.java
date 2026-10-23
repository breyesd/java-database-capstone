package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class DoctorServiceLoginTest {

    private AuthenticationManager authenticationManager;
    private TokenService tokenService;
    private DoctorRepository doctorRepository;
    private BCryptPasswordEncoder encoder;
    private DoctorService doctorService;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        tokenService = Mockito.mock(TokenService.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        AppointmentRepository appointmentRepository = Mockito.mock(AppointmentRepository.class);
        encoder = new BCryptPasswordEncoder(10);
        doctorService = new DoctorService(doctorRepository, appointmentRepository,
                tokenService, authenticationManager, encoder);
    }

    @Test
    void doctorLoginSuccessReturnsToken() {
        Login login = new Login();
        login.setEmail("dr.x@example.com");
        login.setPassword("secret");
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("dr.x@example.com", "secret"))).thenReturn(auth);
        Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))).when(auth).getAuthorities();
        Mockito.when(tokenService.generateToken("dr.x@example.com", "doctor")).thenReturn("jwt.doc");

        ResponseEntity<Map<String, String>> res = doctorService.validateDoctor(login);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("jwt.doc", res.getBody().get("token"));
    }

    @Test
    void doctorLoginWithPatientCredentialsReturns401() {
        Login login = new Login();
        login.setEmail("jane.doe@example.com");
        login.setPassword("passJane1");
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(Mockito.any())).thenReturn(auth);
        Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(auth).getAuthorities();

        ResponseEntity<Map<String, String>> res = doctorService.validateDoctor(login);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("Invalid email or password", res.getBody().get("message"));
    }

    @Test
    void doctorLoginBadCredentials401() {
        Login login = new Login();
        login.setEmail("dr.x@example.com");
        login.setPassword("wrong");
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("bad"));
        ResponseEntity<Map<String, String>> res = doctorService.validateDoctor(login);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("Invalid email or password", res.getBody().get("message"));
    }

    @Test
    void saveDoctorHashesPassword() {
        Mockito.when(doctorRepository.findByEmail("dr.new@example.com")).thenReturn(null);
        Doctor doctor = new Doctor();
        doctor.setEmail("dr.new@example.com");
        doctor.setPassword("plain123");

        assertEquals(1, doctorService.saveDoctor(doctor));
        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        Mockito.verify(doctorRepository).save(captor.capture());
        String stored = captor.getValue().getPassword();
        assertTrue(stored.startsWith("$2"));
        assertTrue(encoder.matches("plain123", stored));
    }

    @Test
    void updateDoctorKeepsExistingPasswordWhenBlank() {
        Doctor existing = new Doctor();
        existing.setId(1L);
        existing.setEmail("dr.x@example.com");
        existing.setPassword("$2a$10$existingHash");
        Mockito.when(doctorRepository.findById(1L)).thenReturn(Optional.of(existing));

        Doctor update = new Doctor();
        update.setId(1L);
        update.setEmail("dr.x@example.com");
        update.setName("New Name");
        update.setPassword("");

        assertEquals(1, doctorService.updateDoctor(update));
        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        Mockito.verify(doctorRepository).save(captor.capture());
        assertEquals("$2a$10$existingHash", captor.getValue().getPassword());
    }
}
