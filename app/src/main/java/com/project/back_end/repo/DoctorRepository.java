package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de {@link Doctor}. Expone la búsqueda por email
 * (login y validación de tokens), los filtros por nombre/especialidad
 * y la lista completa con los slots de disponibilidad eagerly cargados.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Lista todos los doctores con sus slots de disponibilidad eagerly
     * (necesario porque {@code open-in-view=false} y los DTO/JSON
     * serializan {@code availableTimes}).
     *
     * @return todos los doctores con disponibilidad cargada
     */
    @Override
    @EntityGraph(attributePaths = "availableTimes")
    List<Doctor> findAll();

    /**
     * Busca un doctor por su email (identificador de login y subject del JWT).
     *
     * @param email email del doctor
     * @return el doctor encontrado o {@code null} si no existe
     */
    Doctor findByEmail(String email);

    /**
     * Doctores cuyo nombre contenga parcialmente el texto (case-insensitive),
     * con disponibilidad eagerly cargada.
     *
     * @param name fragmento del nombre
     * @return doctores que cumplen el filtro
     */
    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.availableTimes "
            + "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Doctor> findByNameLike(@Param("name") String name);

    /**
     * Doctores por nombre parcial (case-insensitive) y especialidad exacta
     * (case-insensitive), con disponibilidad eagerly cargada.
     *
     * @param name      fragmento del nombre
     * @param specialty especialidad exacta
     * @return doctores que cumplen ambos filtros
     */
    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.availableTimes "
            + "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) "
            + "AND LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
            @Param("name") String name,
            @Param("specialty") String specialty);

    /**
     * Doctores por especialidad exacta (case-insensitive), con
     * disponibilidad eagerly cargada.
     *
     * @param specialty especialidad exacta
     * @return doctores de esa especialidad
     */
    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.availableTimes "
            + "WHERE LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findBySpecialtyIgnoreCase(@Param("specialty") String specialty);
}
