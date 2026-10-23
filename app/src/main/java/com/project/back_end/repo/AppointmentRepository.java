package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio JPA de {@link Appointment}. Expone las consultas por doctor
 * (día/rango de tiempo, con o sin filtro por nombre de paciente), por
 * paciente (historial y filtros por estado), el borrado en cascada lógica
 * por doctor y la actualización de estado al guardar una receta.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Citas de un doctor dentro de un rango de tiempo, con doctor y sus
     * slots de disponibilidad cargados eagerly (evita LazyInitialization
     * con {@code open-in-view=false}).
     *
     * @param doctorId id del doctor
     * @param start    inicio del rango (inclusive)
     * @param end      fin del rango (exclusive)
     * @return citas del doctor en el rango
     */
    @Query("SELECT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.availableTimes "
            + "WHERE a.doctor.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Citas de un doctor en un rango de tiempo cuyo nombre de paciente
     * contenga parcialmente el texto indicado (case-insensitive), con
     * doctor, disponibilidad y paciente cargados eagerly.
     *
     * @param doctorId    id del doctor
     * @param patientName fragmento del nombre del paciente
     * @param start       inicio del rango (inclusive)
     * @param end         fin del rango (exclusive)
     * @return citas que cumplen el filtro
     */
    @Query("SELECT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.availableTimes "
            + "LEFT JOIN FETCH a.patient p "
            + "WHERE a.doctor.id = :doctorId "
            + "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%')) "
            + "AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Elimina todas las citas de un doctor (usado al borrar el doctor).
     *
     * @param doctorId id del doctor
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Historial completo de citas de un paciente.
     *
     * @param patientId id del paciente
     * @return citas del paciente
     */
    List<Appointment> findByPatientId(Long patientId);

    /**
     * Citas de un paciente con un estado concreto, ordenadas por hora.
     *
     * @param patientId id del paciente
     * @param status    estado (0 = scheduled, 1 = completed)
     * @return citas filtradas por estado
     */
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    /**
     * Citas de un paciente cuyo nombre de doctor contenga parcialmente
     * el texto indicado (case-insensitive), con doctor y paciente eagerly.
     *
     * @param doctorName fragmento del nombre del doctor
     * @param patientId  id del paciente
     * @return citas que cumplen el filtro
     */
    @Query("SELECT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.availableTimes "
            + "LEFT JOIN FETCH a.patient p "
            + "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) "
            + "AND a.patient.id = :patientId")
    List<Appointment> filterByDoctorNameAndPatientId(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId);

    /**
     * Citas de un paciente por nombre de doctor y estado, con doctor y
     * paciente eagerly.
     *
     * @param doctorName fragmento del nombre del doctor
     * @param patientId  id del paciente
     * @param status     estado (0 = scheduled, 1 = completed)
     * @return citas que cumplen el filtro
     */
    @Query("SELECT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.availableTimes "
            + "LEFT JOIN FETCH a.patient p "
            + "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) "
            + "AND a.patient.id = :patientId "
            + "AND a.status = :status")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId,
            @Param("status") int status);

    /**
     * Actualiza el estado de una cita por id (usado al guardar una receta).
     *
     * @param status nuevo estado (0 = scheduled, 1 = completed)
     * @param id     id de la cita
     */
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("status") int status, @Param("id") long id);
}
