package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de citas: consulta por día (doctor), reserva y
 * actualización (paciente, validando disponibilidad) y cancelación
 * (solo el dueño). El token va como path variable final; la autenticación
 * y autorización (token + rol) las resuelve el filtro JWT de Spring Security.
 */
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    /**
     * Inyección por constructor de servicios.
     *
     * @param appointmentService servicio de citas
     * @param service            servicio compartido (validación de cita)
     */
    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /**
     * Citas de un doctor en una fecha (dashboard del doctor), con filtro
     * opcional por nombre de paciente ({@code null} = sin filtro).
     * {@code GET /appointments/{date}/{patientName}/{token}}.
     *
     * @param date        fecha {@code yyyy-MM-dd}
     * @param patientName fragmento del nombre o {@code null}
     * @param token       JWT del doctor
     * @return 200 {@code {appointments: [...]}} / 400
     */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(@PathVariable String date,
                                                               @PathVariable String patientName,
                                                               @PathVariable String token) {
        try {
            return ResponseEntity.ok(appointmentService.getAppointment(patientName, LocalDate.parse(date), token));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format, expected yyyy-MM-dd"));
        }
    }

    /**
     * Reserva de cita (paciente). Valida doctor existente y slot libre
     * antes de guardar. {@code POST /appointments/{token}}.
     *
     * @param appointment cita validada por Bean Validation
     * @param token       JWT del paciente
     * @return 201 éxito / 404 doctor inexistente / 409 slot ocupado / 500
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@Valid @RequestBody Appointment appointment,
                                                               @PathVariable String token) {
        int valid = service.validateAppointment(appointment);
        if (valid == -1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Doctor not found"));
        }
        if (valid == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Appointment slot already booked"));
        }
        if (appointmentService.bookAppointment(appointment) == 1) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Appointment Booked"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to book appointment"));
    }

    /**
     * Actualización de cita (paciente dueño). {@code PUT /appointments/{token}}.
     *
     * @param appointment cita actualizada
     * @param token       JWT del paciente
     * @return 200 éxito / 404 / 401 / 409 / 500 según el servicio
     */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@Valid @RequestBody Appointment appointment,
                                                                 @PathVariable String token) {
        return appointmentService.updateAppointment(appointment);
    }

    /**
     * Cancelación de cita (solo el paciente dueño).
     * {@code DELETE /appointments/{id}/{token}}.
     *
     * @param id    id de la cita
     * @param token JWT del paciente
     * @return 200 éxito / 404 no encontrada / 401 no es el dueño / 500
     */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id,
                                                                 @PathVariable String token) {
        return appointmentService.cancelAppointment(id, token);
    }
}
