package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio de pacientes: registro, historial de citas (como
 * {@link AppointmentDTO}), filtros por condición (past/future) y por
 * nombre de doctor, y detalle del paciente autenticado.
 */
@org.springframework.stereotype.Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    /** Estado que la guía asocia a citas pasadas/completadas. */
    private static final int STATUS_PAST = 1;

    /** Estado que la guía asocia a citas futuras/programadas. */
    private static final int STATUS_FUTURE = 0;

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inyección por constructor de dependencias del servicio.
     *
     * @param patientRepository    repositorio de pacientes
     * @param appointmentRepository repositorio de citas
     * @param tokenService         extractor de email del JWT
     * @param passwordEncoder      codificador BCrypt para el registro
     */
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService,
                          PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un paciente nuevo. La contraseña se guarda hasheada
     * (BCrypt) antes de persistir.
     *
     * @param patient paciente a guardar (password en claro)
     * @return {@code 1} éxito, {@code 0} error interno
     */
    @Transactional
    public int createPatient(Patient patient) {
        try {
            patient.setPassword(passwordEncoder.encode(patient.getPassword()));
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            log.error("Error creating patient {}", patient.getEmail(), e);
            return 0;
        }
    }

    /**
     * Historial de citas de un paciente. El id pedido debe coincidir con
     * el que se deduce del token cuando el rol es {@code patient}; para
     * rol {@code doctor} (vista de historial del paciente en el dashboard
     * del doctor) se omite esa comprobación porque el token es del doctor.
     *
     * @param id    id del paciente pedido
     * @param token JWT del rol correspondiente
     * @param user  rol del solicitante: {@code patient} o {@code doctor}
     * @return 200 {@code {appointments: [...]}}; 401 si el rol patient no coincide
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token, String user) {
        try {
            if ("patient".equalsIgnoreCase(user)) {
                String email = tokenService.extractIdentifier(token);
                Patient patient = patientRepository.findByEmail(email);
                if (patient == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Invalid Token or session expired"));
                }
                if (!patient.getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Unauthorized: patient ID does not match token"));
                }
            }
            // Rol doctor: el token ya fue validado como doctor por el controller.
            List<AppointmentDTO> appointments = appointmentRepository.findByPatientId(id).stream()
                    .map(AppointmentDTO::new)
                    .toList();
            return ResponseEntity.ok(Map.of("appointments", appointments));
        } catch (Exception e) {
            log.error("Error fetching appointments for patient {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Filtra las citas de un paciente por condición: {@code past} (estado 1)
     * o {@code future} (estado 0).
     *
     * @param condition condición: {@code past} o {@code future}
     * @param id        id del paciente
     * @return 200 {@code {appointments: [...]}}; 400 si la condición no es válida
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        int status;
        if ("past".equalsIgnoreCase(condition)) {
            status = STATUS_PAST;
        } else if ("future".equalsIgnoreCase(condition)) {
            status = STATUS_FUTURE;
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid condition: " + condition));
        }
        try {
            List<AppointmentDTO> appointments =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status).stream()
                            .map(AppointmentDTO::new)
                            .toList();
            return ResponseEntity.ok(Map.of("appointments", appointments));
        } catch (Exception e) {
            log.error("Error filtering appointments by condition {} for patient {}", condition, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Filtra las citas de un paciente por nombre (parcial) de doctor.
     *
     * @param name      fragmento del nombre del doctor
     * @param patientId id del paciente
     * @return 200 {@code {appointments: [...]}}; 500 en error interno
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        try {
            List<AppointmentDTO> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientId(name, patientId).stream()
                            .map(AppointmentDTO::new)
                            .toList();
            return ResponseEntity.ok(Map.of("appointments", appointments));
        } catch (Exception e) {
            log.error("Error filtering appointments by doctor {} for patient {}", name, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Filtra las citas de un paciente por condición y nombre de doctor.
     *
     * @param condition condición: {@code past} o {@code future}
     * @param name      fragmento del nombre del doctor
     * @param patientId id del paciente
     * @return 200 {@code {appointments: [...]}}; 400 si la condición no es válida
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name,
                                                                         long patientId) {
        int status;
        if ("past".equalsIgnoreCase(condition)) {
            status = STATUS_PAST;
        } else if ("future".equalsIgnoreCase(condition)) {
            status = STATUS_FUTURE;
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid condition: " + condition));
        }
        try {
            List<AppointmentDTO> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status).stream()
                            .map(AppointmentDTO::new)
                            .toList();
            return ResponseEntity.ok(Map.of("appointments", appointments));
        } catch (Exception e) {
            log.error("Error filtering appointments by doctor {} and condition {} for patient {}",
                    name, condition, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Detalle del paciente autenticado (deducido del subject del JWT).
     *
     * @param token JWT del paciente
     * @return 200 {@code {patient: {...}}}; 401 si el token no corresponde a un paciente
     */
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid Token or session expired"));
            }
            return ResponseEntity.ok(Map.of("patient", patient));
        } catch (Exception e) {
            log.error("Error fetching patient details from token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }
}
