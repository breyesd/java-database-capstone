package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService multi-tabla para Spring Security. Resuelve el
 * identificador (username de admin, email de doctor/paciente) probando
 * en orden admin → doctor → paciente y devuelve las autoridades
 * {@code ROLE_ADMIN}, {@code ROLE_DOCTOR} o {@code ROLE_PATIENT}.
 * La contraseña devuelta es el hash almacenado (BCrypt); la compara el
 * {@code DaoAuthenticationProvider}.
 */
@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    /**
     * Inyección por constructor de los repositorios de usuarios.
     *
     * @param adminRepository   repositorio de admins
     * @param doctorRepository  repositorio de doctores
     * @param patientRepository repositorio de pacientes
     */
    public ClinicUserDetailsService(AdminRepository adminRepository,
                                    DoctorRepository doctorRepository,
                                    PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Carga el usuario por identificador (admin, doctor o paciente).
     *
     * @param identifier username de admin o email de doctor/paciente
     * @return UserDetails con la autoridad del rol encontrado
     * @throws UsernameNotFoundException si el identificador no existe en ninguna tabla
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(identifier);
        if (admin != null) {
            return User.withUsername(admin.getUsername())
                    .password(admin.getPassword())
                    .authorities("ROLE_ADMIN")
                    .build();
        }
        Doctor doctor = doctorRepository.findByEmail(identifier);
        if (doctor != null) {
            return User.withUsername(doctor.getEmail())
                    .password(doctor.getPassword())
                    .authorities("ROLE_DOCTOR")
                    .build();
        }
        Patient patient = patientRepository.findByEmail(identifier);
        if (patient != null) {
            return User.withUsername(patient.getEmail())
                    .password(patient.getPassword())
                    .authorities("ROLE_PATIENT")
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + identifier);
    }
}
