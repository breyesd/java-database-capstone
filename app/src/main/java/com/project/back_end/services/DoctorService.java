package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio de doctores: disponibilidad por día, CRUD
 * (solo admin, vía token validado en el controller), login de doctor
 * y filtros por nombre/especialidad/hora AM-PM.
 */
@org.springframework.stereotype.Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inyección por constructor de dependencias del servicio.
     *
     * @param doctorRepository   repositorio de doctores
     * @param appointmentRepository repositorio de citas (slots ocupados)
     * @param tokenService       generador de JWT para el login
     * @param authenticationManager autenticador de Spring Security para el login
     * @param passwordEncoder    codificador BCrypt para las contraseñas de doctores
     */
    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService,
                         AuthenticationManager authenticationManager,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Devuelve los slots libres de un doctor para una fecha: parte de
     * {@code availableTimes} y elimina los que ya tienen una cita
     * empezando a esa hora.
     *
     * @param doctorId id del doctor
     * @param date     fecha a consultar
     * @return lista de slots {@code "HH:mm-HH:mm"} disponibles
     */
    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        try {
            Optional<Doctor> doctor = doctorRepository.findById(doctorId);
            if (doctor.isEmpty()) {
                return List.of();
            }
            List<String> available = new ArrayList<>(doctor.get().getAvailableTimes());
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            List<Appointment> booked =
                    appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
            List<String> bookedTimes = booked.stream()
                    .map(a -> a.getAppointmentTime().toLocalTime().toString())
                    .toList();
            available.removeIf(slot -> bookedTimes.contains(slot.split("-")[0]));
            return available;
        } catch (Exception e) {
            log.error("Error fetching doctor availability for id {} on {}", doctorId, date, e);
            return List.of();
        }
    }

    /**
     * Da de alta un doctor si su email no existe aún. La contraseña se
     * guarda hasheada (BCrypt) antes de persistir.
     *
     * @param doctor doctor a guardar (password en claro)
     * @return {@code 1} éxito, {@code -1} email ya registrado, {@code 0} error interno
     */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Error saving doctor {}", doctor.getEmail(), e);
            return 0;
        }
    }

    /**
     * Actualiza un doctor existente por id. Si el password viene vacío o
     * nulo se conserva el hash existente; si viene nuevo se hashea (BCrypt).
     *
     * @param doctor doctor con los datos actualizados
     * @return {@code 1} éxito, {@code -1} no encontrado, {@code 0} error interno
     */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || doctorRepository.findById(doctor.getId()).isEmpty()) {
                return -1;
            }
            if (doctor.getPassword() == null || doctor.getPassword().isBlank()) {
                doctor.setPassword(doctorRepository.findById(doctor.getId()).get().getPassword());
            } else {
                doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Error updating doctor {}", doctor.getId(), e);
            return 0;
        }
    }

    /**
     * Lista todos los doctores (con disponibilidad cargada).
     *
     * @return lista completa de doctores
     */
    @Transactional
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Borra un doctor y todas sus citas asociadas.
     *
     * @param id id del doctor
     * @return {@code 1} éxito, {@code -1} no encontrado, {@code 0} error interno
     */
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (doctorRepository.findById(id).isEmpty()) {
                return -1;
            }
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            log.error("Error deleting doctor {}", id, e);
            return 0;
        }
    }

    /**
     * Valida el login de un doctor por email y contraseña vía
     * {@link AuthenticationManager} (usuarios cargados por
     * {@code ClinicUserDetailsService}).
     *
     * @param login credenciales (email + password)
     * @return 200 {@code {token}} si es válido; 401 si no; 500 en error interno
     */
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword()));
            return ResponseEntity.ok(Map.of("token", tokenService.generateToken(login.getEmail(), "doctor")));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            log.error("Error validating doctor login {}", login.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    /**
     * Busca doctores por fragmento de nombre.
     *
     * @param name fragmento del nombre
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        return Map.of("doctors", doctorRepository.findByNameLike(name));
    }

    /**
     * Filtra doctores por nombre, especialidad y disponibilidad AM/PM.
     *
     * @param name      fragmento del nombre
     * @param specialty especialidad exacta
     * @param amOrPm    {@code AM} o {@code PM}
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors =
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    /**
     * Filtra doctores por nombre y disponibilidad AM/PM.
     *
     * @param name   fragmento del nombre
     * @param amOrPm {@code AM} o {@code PM}
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    /**
     * Filtra doctores por nombre y especialidad.
     *
     * @param name      fragmento del nombre
     * @param specialty especialidad exacta
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        return Map.of("doctors",
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty));
    }

    /**
     * Filtra doctores por especialidad y disponibilidad AM/PM.
     *
     * @param specialty especialidad exacta
     * @param amOrPm    {@code AM} o {@code PM}
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    /**
     * Filtra doctores por especialidad.
     *
     * @param specialty especialidad exacta
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        return Map.of("doctors", doctorRepository.findBySpecialtyIgnoreCase(specialty));
    }

    /**
     * Filtra doctores por disponibilidad AM/PM.
     *
     * @param amOrPm {@code AM} o {@code PM}
     * @return mapa {@code {doctors: [...]}}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return Map.of("doctors", filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    /**
     * Se queda solo con los doctores que tengan algún slot cuyo inicio
     * caiga en AM (hora &lt; 12) o PM (hora ≥ 12), según {@code amOrPm}.
     *
     * @param doctors doctores a filtrar
     * @param amOrPm  {@code AM} o {@code PM} (case-insensitive)
     * @return doctores filtrados
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) {
            return doctors;
        }
        boolean wantAm = "AM".equalsIgnoreCase(amOrPm);
        return doctors.stream()
                .filter(d -> d.getAvailableTimes().stream().anyMatch(slot -> {
                    try {
                        int startHour = Integer.parseInt(slot.split("-")[0].split(":")[0]);
                        return wantAm ? startHour < 12 : startHour >= 12;
                    } catch (NumberFormatException | DateTimeParseException e) {
                        return false;
                    }
                }))
                .toList();
    }
}
