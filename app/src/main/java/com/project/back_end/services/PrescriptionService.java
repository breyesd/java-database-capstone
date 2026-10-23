package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio de recetas (MongoDB). Guarda una receta por cita
 * (rechaza duplicados) y recupera las recetas de una cita; el marcado
 * de la cita como completada lo orquesta el controller con
 * {@code AppointmentService.changeStatus}.
 */
@org.springframework.stereotype.Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    /**
     * Inyección por constructor del repositorio de recetas.
     *
     * @param prescriptionRepository repositorio MongoDB de recetas
     */
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * Guarda una receta si la cita aún no tiene ninguna.
     *
     * @param prescription receta a guardar
     * @return 201 éxito; 400 si ya existe una receta para la cita; 500 error interno
     */
    @Transactional
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        try {
            List<Prescription> existing =
                    prescriptionRepository.findByAppointmentId(prescription.getAppointmentId());
            if (existing != null && !existing.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Prescription already exists for this appointment"));
            }
            prescriptionRepository.save(prescription);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Prescription saved"));
        } catch (Exception e) {
            log.error("Error saving prescription for appointment {}",
                    prescription.getAppointmentId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Recupera las recetas de una cita (el frontend espera un array
     * aunque haya una sola).
     *
     * @param appointmentId id de la cita en MySQL
     * @return 200 {@code {prescription: [...]}}; 500 en error interno
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        try {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
            return ResponseEntity.ok(Map.of("prescription", prescriptions == null ? List.of() : prescriptions));
        } catch (Exception e) {
            log.error("Error fetching prescription for appointment {}", appointmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }
}
