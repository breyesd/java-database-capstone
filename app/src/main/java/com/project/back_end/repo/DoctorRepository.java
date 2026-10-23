package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de {@link Doctor}. Expone la búsqueda por email,
 * usada por el login de doctor y por la validación de tokens de rol doctor.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Busca un doctor por su email (identificador de login y subject del JWT).
     *
     * @param email email del doctor
     * @return el doctor encontrado o {@code null} si no existe
     */
    Doctor findByEmail(String email);
}
