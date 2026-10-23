package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Servicio compartido de autenticación y reglas de negocio transversales:
 * login de admin y paciente vía Spring Security ({@link AuthenticationManager}),
 * filtros de doctores y validación de disponibilidad de citas.
 * <p>Nota: {@code filterPatient} depende de {@code PatientService};
 * {@code filterDoctor} y {@code validateAppointment} de {@code DoctorService}.
 * No hay ciclos: esos services no dependen de esta clase.</p>
 * <p>{@code validateToken} queda obsoleto: los controllers pasarán a usar
 * el filtro JWT de seguridad (se elimina en una tarea posterior).</p>
 */
@org.springframework.stereotype.Service
public class Service {

    private static final Logger log = LoggerFactory.getLogger(Service.class);

    /** Mensaje estándar de token inválido/expirado (401). */
    private static final String INVALID_TOKEN = "Invalid Token or session expired";

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final AuthenticationManager authenticationManager;

    /**
     * Inyección por constructor de dependencias compartidas.
     *
     * @param tokenService          generador/validador de JWT
     * @param adminRepository       repositorio de admins
     * @param doctorRepository      repositorio de doctores
     * @param patientRepository     repositorio de pacientes
     * @param doctorService         servicio de doctores (filtros, disponibilidad)
     * @param patientService        servicio de pacientes (filtros de citas)
     * @param authenticationManager gestor de autenticación (login admin/paciente)
     */
    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService,
                   AuthenticationManager authenticationManager) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Valida un token JWT para un rol concreto.
     *
     * @param token token JWT a validar
     * @param user  rol esperado: {@code admin}, {@code doctor} o {@code patient}
     * @return 200 con mapa vacío si es válido; 401 {@code {message}} si no
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        boolean isValid = tokenService.validateToken(token, user);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", INVALID_TOKEN));
        }
        return ResponseEntity.ok(Map.of());
    }

    /**
     * Valida el login de un admin por username y contraseña vía
     * {@link AuthenticationManager} (usuarios cargados por
     * {@code ClinicUserDetailsService}).
     *
     * @param receivedAdmin credenciales (username + password)
     * @return 200 {@code {token}} si es válido; 401 si no; 500 error interno
     */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(receivedAdmin.getUsername(), receivedAdmin.getPassword()));
            return ResponseEntity.ok(Map.of("token", tokenService.generateToken(receivedAdmin.getUsername(), "admin")));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        } catch (Exception e) {
            log.error("Error validating admin login {}", receivedAdmin.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    /**
     * Filtra doctores combinando nombre, especialidad y hora AM/PM.
     * Los valores {@code null}, vacíos o {@code "null"} (string que el
     * frontend envía cuando no hay filtro) se tratan como ausentes; sin
     * ningún filtro se devuelven todos los doctores.
     *
     * @param name      fragmento del nombre o ausente
     * @param specialty especialidad exacta o ausente
     * @param time      {@code AM}/{@code PM} o ausente
     * @return mapa {@code {doctors: [...]}}
     */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = !isMissing(name);
        boolean hasSpecialty = !isMissing(specialty);
        boolean hasTime = !isMissing(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (hasTime && hasSpecialty) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (hasName) {
            return doctorService.findDoctorByName(name);
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }
        if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        return Map.of("doctors", doctorService.getDoctors());
    }

    /**
     * Valida que la hora pedida para una cita coincida con el inicio de
     * un slot libre del doctor en esa fecha.
     *
     * @param appointment cita a validar (debe traer doctor.id y appointmentTime)
     * @return {@code 1} slot válido; {@code 0} slot ocupado/inválido;
     *         {@code -1} doctor inexistente
     */
    public int validateAppointment(Appointment appointment) {
        try {
            if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                    || appointment.getAppointmentTime() == null) {
                return -1;
            }
            Long doctorId = appointment.getDoctor().getId();
            if (doctorRepository.findById(doctorId).isEmpty()) {
                return -1;
            }
            String requested = appointment.getAppointmentTime().toLocalTime().toString();
            return doctorService.getDoctorAvailability(doctorId, appointment.getAppointmentTime().toLocalDate())
                    .stream()
                    .anyMatch(slot -> slot.split("-")[0].equals(requested)) ? 1 : 0;
        } catch (Exception e) {
            log.error("Error validating appointment", e);
            return 0;
        }
    }

    /**
     * Comprueba que un paciente no exista ya por email o teléfono
     * (usado antes del registro).
     *
     * @param patient paciente a validar
     * @return {@code true} si NO existe (se puede registrar); {@code false} si ya existe
     */
    public boolean validatePatient(Patient patient) {
        try {
            return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
        } catch (Exception e) {
            log.error("Error validating patient {}", patient.getEmail(), e);
            return false;
        }
    }

    /**
     * Valida el login de un paciente por email y contraseña vía
     * {@link AuthenticationManager} (usuarios cargados por
     * {@code ClinicUserDetailsService}).
     *
     * @param login credenciales (email + password)
     * @return 200 {@code {token}} si es válido; 401 si no; 500 error interno
     */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword()));
            return ResponseEntity.ok(Map.of("token", tokenService.generateToken(login.getEmail(), "patient")));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            log.error("Error validating patient login {}", login.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    /**
     * Filtra las citas del paciente deducido del token por condición
     * (past/future), nombre de doctor, ambos o ninguno. Los valores
     * {@code null}, vacíos o {@code "null"} se tratan como ausentes;
     * sin ningún filtro se devuelve el historial completo.
     *
     * @param condition condición ({@code past}/{@code future}) o ausente
     * @param name      fragmento del nombre del doctor o ausente
     * @param token     JWT del paciente
     * @return 200 {@code {appointments: [...]}}; 401 token inválido; 400 condición inválida
     */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            boolean hasCondition = !isMissing(condition);
            boolean hasName = !isMissing(name);

            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", INVALID_TOKEN));
            }
            Long id = patient.getId();

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, id);
            }
            if (hasCondition) {
                return patientService.filterByCondition(condition, id);
            }
            if (hasName) {
                return patientService.filterByDoctor(name, id);
            }
            return patientService.getPatientAppointment(id, token, "patient");
        } catch (Exception e) {
            log.error("Error filtering patient appointments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Normaliza los valores de filtro que el frontend pone en la URL:
     * {@code null}, cadena vacía o el literal {@code "null"} significan
     * "sin filtro".
     *
     * @param value valor de path variable
     * @return {@code true} si no hay filtro aplicable
     */
    private boolean isMissing(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value);
    }
}
