package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de recetas (MongoDB): guardado (solo doctor, marca la
 * cita como completada) y consulta por id de cita.
 */
@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;

    /**
     * Inyección por constructor de servicios.
     *
     * @param prescriptionService servicio de recetas
     * @param service             servicio compartido (validación de token)
     * @param appointmentService  servicio de citas (marcar status = 1)
     */
    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    /**
     * Guarda una receta (solo doctor). Si se guarda, la cita asociada
     * pasa a estado 1 (completed). {@code POST /prescription/{token}}.
     *
     * @param prescription receta validada por Bean Validation
     * @param token        JWT del doctor
     * @return 201 éxito / 400 ya existe / 401 / 500
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @Valid @RequestBody Prescription prescription,
            @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        ResponseEntity<Map<String, String>> saved = prescriptionService.savePrescription(prescription);
        if (saved.getStatusCode().is2xxSuccessful()) {
            appointmentService.changeStatus(1, prescription.getAppointmentId());
        }
        return saved;
    }

    /**
     * Receta de una cita (solo doctor). {@code GET /prescription/{appointmentId}/{token}}.
     *
     * @param appointmentId id de la cita en MySQL
     * @param token         JWT del doctor
     * @return 200 {@code {prescription: [...]}} / 401 / 500
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(@PathVariable Long appointmentId,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("message", validation.getBody().get("message")));
        }
        return prescriptionService.getPrescription(appointmentId);
    }
}
