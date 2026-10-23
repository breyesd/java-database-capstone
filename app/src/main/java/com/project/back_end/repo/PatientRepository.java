package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de {@link Patient}. Expone la búsqueda por email,
 * usada por el login de paciente y por la validación de tokens de rol patient.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Busca un paciente por su email (identificador de login y subject del JWT).
     *
     * @param email email del paciente
     * @return el paciente encontrado o {@code null} si no existe
     */
    Patient findByEmail(String email);
}
