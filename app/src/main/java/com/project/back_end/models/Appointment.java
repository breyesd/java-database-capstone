package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad JPA que representa una cita entre un doctor y un paciente.
 * Mapea la tabla {@code appointments}; la duración es fija de 1 hora
 * y el estado usa 0 = scheduled, 1 = completed.
 */
@Entity
public class Appointment {

  // @Entity annotation:
//    - Marks the class as a JPA entity, meaning it represents a table in the database.
//    - Required for persistence frameworks (e.g., Hibernate) to map the class to a database table.

// 1. 'id' field:
//    - Type: private Long
//    - Description:
//      - Represents the unique identifier for each appointment.
//      - The @Id annotation marks it as the primary key.
//      - The @GeneratedValue(strategy = GenerationType.IDENTITY) annotation auto-generates the ID value when a new record is inserted into the database.

// 2. 'doctor' field:
//    - Type: private Doctor
//    - Description:
//      - Represents the doctor assigned to this appointment.
//      - The @ManyToOne annotation defines the relationship, indicating many appointments can be linked to one doctor.
//      - The @NotNull annotation ensures that an appointment must be associated with a doctor when created.

// 3. 'patient' field:
//    - Type: private Patient
//    - Description:
//      - Represents the patient assigned to this appointment.
//      - The @ManyToOne annotation defines the relationship, indicating many appointments can be linked to one patient.
//      - The @NotNull annotation ensures that an appointment must be associated with a patient when created.

// 4. 'appointmentTime' field:
//    - Type: private LocalDateTime
//    - Description:
//      - Represents the date and time when the appointment is scheduled to occur.
//      - The @Future annotation ensures that the appointment time is always in the future when the appointment is created.
//      - It uses LocalDateTime, which includes both the date and time for the appointment.

// 5. 'status' field:
//    - Type: private int
//    - Description:
//      - Represents the current status of the appointment. It is an integer where:
//        - 0 means the appointment is scheduled.
//        - 1 means the appointment has been completed.
//      - The @NotNull annotation ensures that the status field is not null.

// 6. 'getEndTime' method:
//    - Type: private LocalDateTime
//    - Description:
//      - This method is a transient field (not persisted in the database).
//      - It calculates the end time of the appointment by adding one hour to the start time (appointmentTime).
//      - It is used to get an estimated appointment end time for display purposes.

// 7. 'getAppointmentDate' method:
//    - Type: private LocalDate
//    - Description:
//      - This method extracts only the date part from the appointmentTime field.
//      - It returns a LocalDate object representing just the date (without the time) of the scheduled appointment.

// 8. 'getAppointmentTimeOnly' method:
//    - Type: private LocalTime
//    - Description:
//      - This method extracts only the time part from the appointmentTime field.
//      - It returns a LocalTime object representing just the time (without the date) of the scheduled appointment.

// 9. Constructor(s):
//    - A no-argument constructor is implicitly provided by JPA for entity creation.
//    - A parameterized constructor can be added as needed to initialize fields.

// 10. Getters and Setters:
//    - Standard getter and setter methods are provided for accessing and modifying the fields: id, doctor, patient, appointmentTime, status, etc.

    /** Identificador autoincremental (primary key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Doctor de la cita; relación ManyToOne obligatoria. */
    @ManyToOne
    @NotNull
    private Doctor doctor;

    /** Paciente de la cita; relación ManyToOne obligatoria. */
    @ManyToOne
    @NotNull
    private Patient patient;

    /** Fecha y hora de inicio; debe ser futura al momento de crear la cita. */
    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime;

    /**
     * Estado de la cita: 0 = scheduled, 1 = completed.
     * Primitivo int (no admite {@code @NotNull}); el estado inicial es 0
     * y pasa a 1 al guardar la receta asociada.
     */
    private int status;

    /** Constructor vacío requerido por JPA. */
    public Appointment() {
    }

    /**
     * Constructor para crear una cita con sus datos principales.
     *
     * @param doctor          doctor asignado
     * @param patient         paciente asignado
     * @param appointmentTime fecha y hora de inicio (futura)
     * @param status          0 = scheduled, 1 = completed
     */
    public Appointment(Doctor doctor, Patient patient, LocalDateTime appointmentTime, int status) {
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    /** @return identificador de la cita */
    public Long getId() {
        return id;
    }

    /** @param id identificador a asignar */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return doctor de la cita */
    public Doctor getDoctor() {
        return doctor;
    }

    /** @param doctor doctor a asignar */
    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    /** @return paciente de la cita */
    public Patient getPatient() {
        return patient;
    }

    /** @param patient paciente a asignar */
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    /** @return fecha y hora de inicio de la cita */
    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    /** @param appointmentTime fecha y hora de inicio a asignar */
    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    /** @return estado actual (0 = scheduled, 1 = completed) */
    public int getStatus() {
        return status;
    }

    /** @param status estado a asignar (0 = scheduled, 1 = completed) */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Hora de fin de la cita: inicio + 1 hora (duración fija del sistema).
     * Método transitorio (no persiste); pensado para la vista/DTO.
     *
     * @return hora de fin, o {@code null} si aún no hay hora de inicio
     */
    @Transient
    public LocalDateTime getEndTime() {
        return appointmentTime == null ? null : appointmentTime.plusHours(1);
    }

    /**
     * Extrae solo la parte de fecha de {@code appointmentTime}.
     * Método transitorio (no persiste); pensado para la vista/DTO.
     *
     * @return fecha de la cita, o {@code null} si aún no hay hora de inicio
     */
    @Transient
    public LocalDate getAppointmentDate() {
        return appointmentTime == null ? null : appointmentTime.toLocalDate();
    }

    /**
     * Extrae solo la parte de hora de {@code appointmentTime}.
     * Método transitorio (no persiste); pensado para la vista/DTO.
     *
     * @return hora de la cita, o {@code null} si aún no hay hora de inicio
     */
    @Transient
    public LocalTime getAppointmentTimeOnly() {
        return appointmentTime == null ? null : appointmentTime.toLocalTime();
    }

}
