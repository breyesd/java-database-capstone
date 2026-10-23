package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class ClinicUserDetailsServiceTest {

    private AdminRepository adminRepository;
    private DoctorRepository doctorRepository;
    private PatientRepository patientRepository;
    private ClinicUserDetailsService service;

    @BeforeEach
    void setUp() {
        adminRepository = Mockito.mock(AdminRepository.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        patientRepository = Mockito.mock(PatientRepository.class);
        service = new ClinicUserDetailsService(adminRepository, doctorRepository, patientRepository);
    }

    @Test
    void loadsAdminWithRoleAdmin() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("$2a$10$hash");
        Mockito.when(adminRepository.findByUsername("admin")).thenReturn(admin);

        UserDetails user = service.loadUserByUsername("admin");
        assertEquals("admin", user.getUsername());
        assertEquals("$2a$10$hash", user.getPassword());
        assertTrue(user.getAuthorities().iterator().next().getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadsDoctorWhenNotAdmin() {
        Mockito.when(adminRepository.findByUsername("dr.x@example.com")).thenReturn(null);
        Doctor doctor = new Doctor();
        doctor.setEmail("dr.x@example.com");
        doctor.setPassword("h");
        Mockito.when(doctorRepository.findByEmail("dr.x@example.com")).thenReturn(doctor);

        UserDetails user = service.loadUserByUsername("dr.x@example.com");
        assertEquals("ROLE_DOCTOR", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadsPatientWhenNeitherAdminNorDoctor() {
        Mockito.when(adminRepository.findByUsername("p@example.com")).thenReturn(null);
        Mockito.when(doctorRepository.findByEmail("p@example.com")).thenReturn(null);
        Patient patient = new Patient();
        patient.setEmail("p@example.com");
        patient.setPassword("h");
        Mockito.when(patientRepository.findByEmail("p@example.com")).thenReturn(patient);

        UserDetails user = service.loadUserByUsername("p@example.com");
        assertEquals("ROLE_PATIENT", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void unknownIdentifierThrows() {
        Mockito.when(adminRepository.findByUsername("nope")).thenReturn(null);
        Mockito.when(doctorRepository.findByEmail("nope")).thenReturn(null);
        Mockito.when(patientRepository.findByEmail("nope")).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nope"));
    }
}
