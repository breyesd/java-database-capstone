package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio de citas: reserva, actualización (validando doctor,
 * slot y pertenencia), cancelación (solo el dueño) y consulta por día
 * para el dashboard del doctor.
 */
@org.springframework.stereotype.Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Inyección por constructor de dependencias del servicio.
     *
     * @param appointmentRepository repositorio de citas
     * @param service               servicio compartido ({@code validateAppointment})
     * @param tokenService          extractor de email del JWT
     * @param patientRepository     repositorio de pacientes (ownership)
     * @param doctorRepository      repositorio de doctores (consulta por día)
     */
    public AppointmentService(AppointmentRepository appointmentRepository,
                              Service service,
                              TokenService tokenService,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Reserva una cita nueva (el controller ya validó doctor y slot).
     *
     * @param appointment cita a guardar
     * @return {@code 1} éxito, {@code 0} error interno
     */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            log.error("Error booking appointment", e);
            return 0;
        }
    }

    /**
     * Actualiza una cita existente: la cita debe existir, pertenecer al
     * mismo paciente y el nuevo horario debe ser válido (doctor existente
     * y slot libre).
     *
     * @param appointment cita con los datos actualizados
     * @return 200 éxito; 404 no encontrada; 401 paciente distinto;
     *         404 doctor inexistente; 409 slot ocupado; 500 error interno
     */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        try {
            Optional<Appointment> existing = appointmentRepository.findById(appointment.getId());
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Appointment not found"));
            }
            Patient existingPatient = existing.get().getPatient();
            if (appointment.getPatient() == null || existingPatient == null
                    || !existingPatient.getId().equals(appointment.getPatient().getId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Unauthorized: appointment belongs to another patient"));
            }
            int valid = service.validateAppointment(appointment);
            if (valid == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Doctor not found"));
            }
            if (valid == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Appointment slot already booked"));
            }
            appointmentRepository.save(appointment);
            return ResponseEntity.ok(Map.of("message", "Appointment updated"));
        } catch (Exception e) {
            log.error("Error updating appointment {}", appointment.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Cancela (elimina) una cita. Solo el paciente cuyo email figura en
     * el token puede cancelarla.
     *
     * @param id    id de la cita
     * @param token JWT del paciente dueño
     * @return 200 éxito; 404 no encontrada; 401 no es el dueño; 500 error interno
     */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        try {
            Optional<Appointment> existing = appointmentRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Appointment not found"));
            }
            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null || existing.get().getPatient() == null
                    || !patient.getId().equals(existing.get().getPatient().getId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Unauthorized: you can only cancel your own appointments"));
            }
            appointmentRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("message", "Appointment cancelled"));
        } catch (Exception e) {
            log.error("Error cancelling appointment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Citas de un doctor en una fecha concreta, con filtro opcional por
     * nombre (parcial) de paciente; {@code pname} nulo, vacío o
     * {@code "null"} significa sin filtro.
     *
     * @param pname  fragmento del nombre del paciente o {@code null}
     * @param date   fecha de las citas
     * @param token  JWT del doctor (su id se deduce del subject)
     * @return mapa {@code {appointments: [...]}}; lista vacía si el doctor no existe
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        try {
            String email = tokenService.extractIdentifier(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null) {
                return Map.of("appointments", List.of());
            }
            LocalDate startOfDay = date;
            LocalDate endOfDay = date.plusDays(1);
            List<Appointment> appointments;
            if (pname == null || pname.isBlank() || "null".equalsIgnoreCase(pname)) {
                appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        doctor.getId(), startOfDay.atStartOfDay(), endOfDay.atStartOfDay());
            } else {
                appointments =
                        appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname, startOfDay.atStartOfDay(), endOfDay.atStartOfDay());
            }
            return Map.of("appointments", appointments);
        } catch (Exception e) {
            log.error("Error fetching appointments for doctor on {}", date, e);
            return Map.of("appointments", List.of());
        }
    }

    /**
     * Marca el estado de una cita (usado al guardar una receta: estado 1).
     *
     * @param status nuevo estado (0 = scheduled, 1 = completed)
     * @param id     id de la cita
     */
    @Transactional
    public void changeStatus(int status, long id) {
        appointmentRepository.updateStatus(status, id);
    }
}
