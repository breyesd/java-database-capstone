package com.project.back_end.DTO;

import com.project.back_end.models.Appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO que simplifica la representación de una cita para las respuestas
 * de la API: expone los datos planos del doctor y del paciente más los
 * campos derivados de la fecha/hora, sin relaciones JPA ni anotaciones
 * de persistencia.
 */
public class AppointmentDTO {

    /** Identificador de la cita. */
    private Long id;

    /** Id del doctor asignado. */
    private Long doctorId;

    /** Nombre completo del doctor. */
    private String doctorName;

    /** Id del paciente. */
    private Long patientId;

    /** Nombre completo del paciente. */
    private String patientName;

    /** Email del paciente. */
    private String patientEmail;

    /** Teléfono del paciente. */
    private String patientPhone;

    /** Dirección del paciente. */
    private String patientAddress;

    /** Fecha y hora completas de la cita. */
    private LocalDateTime appointmentTime;

    /** Estado: 0 = scheduled, 1 = completed. */
    private int status;

    /** Fecha extraída de {@code appointmentTime}. */
    private LocalDate appointmentDate;

    /** Hora extraída de {@code appointmentTime}. */
    private LocalTime appointmentTimeOnly;

    /** Fin calculado: {@code appointmentTime} + 1 hora. */
    private LocalDateTime endTime;

    /**
     * Construye el DTO a partir de una entidad {@link Appointment},
     * extrayendo los datos aplanados del doctor y del paciente y
     * calculando los campos derivados de la fecha/hora.
     *
     * @param appointment entidad de cita a convertir
     */
    public AppointmentDTO(Appointment appointment) {
        this.id = appointment.getId();
        this.doctorId = appointment.getDoctor() == null ? null : appointment.getDoctor().getId();
        this.doctorName = appointment.getDoctor() == null ? null : appointment.getDoctor().getName();
        this.patientId = appointment.getPatient() == null ? null : appointment.getPatient().getId();
        this.patientName = appointment.getPatient() == null ? null : appointment.getPatient().getName();
        this.patientEmail = appointment.getPatient() == null ? null : appointment.getPatient().getEmail();
        this.patientPhone = appointment.getPatient() == null ? null : appointment.getPatient().getPhone();
        this.patientAddress = appointment.getPatient() == null ? null : appointment.getPatient().getAddress();
        this.appointmentTime = appointment.getAppointmentTime();
        this.status = appointment.getStatus();
        this.appointmentDate = appointment.getAppointmentDate();
        this.appointmentTimeOnly = appointment.getAppointmentTimeOnly();
        this.endTime = appointment.getEndTime();
    }

    /** @return identificador de la cita */
    public Long getId() {
        return id;
    }

    /** @return id del doctor */
    public Long getDoctorId() {
        return doctorId;
    }

    /** @return nombre del doctor */
    public String getDoctorName() {
        return doctorName;
    }

    /** @return id del paciente */
    public Long getPatientId() {
        return patientId;
    }

    /** @return nombre del paciente */
    public String getPatientName() {
        return patientName;
    }

    /** @return email del paciente */
    public String getPatientEmail() {
        return patientEmail;
    }

    /** @return teléfono del paciente */
    public String getPatientPhone() {
        return patientPhone;
    }

    /** @return dirección del paciente */
    public String getPatientAddress() {
        return patientAddress;
    }

    /** @return fecha y hora de la cita */
    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    /** @return estado de la cita (0 = scheduled, 1 = completed) */
    public int getStatus() {
        return status;
    }

    /** @return fecha de la cita (derivada) */
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    /** @return hora de la cita (derivada) */
    public LocalTime getAppointmentTimeOnly() {
        return appointmentTimeOnly;
    }

    /** @return hora de fin (derivada, inicio + 1 h) */
    public LocalDateTime getEndTime() {
        return endTime;
    }
}
