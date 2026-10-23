package com.project.back_end.repo;

import com.project.back_end.models.Prescription;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio MongoDB de {@link Prescription} (colección {@code prescriptions}).
 * {@code appointmentId} es la referencia lógica por valor a la cita en MySQL.
 */
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    /**
     * Recetas asociadas a una cita concreta de MySQL.
     *
     * @param appointmentId id de la cita
     * @return lista de recetas para esa cita (vacía si no hay)
     */
    List<Prescription> findByAppointmentId(Long appointmentId);
}
