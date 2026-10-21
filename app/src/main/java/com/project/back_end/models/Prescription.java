package com.project.back_end.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB que representa una receta médica.
 * Se persiste en la colección {@code prescriptions}; {@code appointmentId}
 * es una referencia lógica por valor a la cita en MySQL (sin FK cross-DB).
 */
@Document(collection = "prescriptions")
public class Prescription {

  // @Document annotation:
//    - Marks the class as a MongoDB document (a collection in MongoDB).
//    - The collection name is specified as "prescriptions" to map this class to the "prescriptions" collection in MongoDB.

// 1. 'id' field:
//    - Type: private String
//    - Description:
//      - Represents the unique identifier for each prescription.
//      - The @Id annotation marks it as the primary key in the MongoDB collection.
//      - The id is of type String, which is commonly used for MongoDB's ObjectId as it stores IDs as strings in the database.

// 2. 'patientName' field:
//    - Type: private String
//    - Description:
//      - Represents the name of the patient receiving the prescription.
//      - The @NotNull annotation ensures that the patient name is required.
//      - The @Size(min = 3, max = 100) annotation ensures that the name length is between 3 and 100 characters, ensuring a reasonable name length.

// 3. 'appointmentId' field:
//    - Type: private Long
//    - Description:
//      - Represents the ID of the associated appointment where the prescription was given.
//      - The @NotNull annotation ensures that the appointment ID is required for the prescription.

// 4. 'medication' field:
//    - Type: private String
//    - Description:
//      - Represents the medication prescribed to the patient.
//      - The @NotNull annotation ensures that the medication name is required.
//      - The @Size(min = 3, max = 100) annotation ensures that the medication name is between 3 and 100 characters, which ensures meaningful medication names.

// 5. 'dosage' field:
//    - Type: private String
//    - Description:
//      - Represents the dosage information for the prescribed medication.
//      - The @NotNull annotation ensures that the dosage information is provided.

// 6. 'doctorNotes' field:
//    - Type: private String
//    - Description:
//      - Represents any additional notes or instructions from the doctor regarding the prescription.
//      - The @Size(max = 200) annotation ensures that the doctor's notes do not exceed 200 characters, providing a reasonable limit for additional notes.

// 7. Constructors:
//    - The class includes a no-argument constructor (default constructor) and a parameterized constructor that initializes the fields: patientName, medication, dosage, doctorNotes, and appointmentId.

// 8. Getters and Setters:
//    - Standard getter and setter methods are provided for all fields: id, patientName, medication, dosage, doctorNotes, and appointmentId.
//    - These methods allow access and modification of the fields of the Prescription class.

    /** Identificador MongoDB (_id); se genera automáticamente como ObjectId. */
    @Id
    private String id;

    /**
     * Nombre del paciente (instantánea desnormalizada de MySQL, no el objeto completo):
     * evita joins cross-DB y datos obsoletos si el perfil del paciente cambia.
     */
    @NotNull
    @Size(min = 3, max = 100)
    private String patientName;

    /**
     * Referencia lógica a la cita en MySQL (sin FK entre motores);
     * solo se resuelve por valor al buscar la receta de una cita.
     */
    @NotNull
    private Long appointmentId;

    /** Nombre del medicamento; obligatorio, entre 3 y 100 caracteres. */
    @NotNull
    @Size(min = 3, max = 100)
    private String medication;

    /** Indicaciones de dosificación; obligatorio, entre 3 y 20 caracteres. */
    @NotNull
    @Size(min = 3, max = 20)
    private String dosage;

    /** Notas opcionales del doctor; máximo 200 caracteres, puede ser null. */
    @Size(max = 200)
    private String doctorNotes;

    /** Constructor vacío requerido por Spring Data MongoDB al deserializar. */
    public Prescription() {
    }

    /**
     * Constructor con los campos principales de una receta.
     *
     * @param patientName    nombre del paciente
     * @param medication     medicamento prescrito
     * @param dosage         dosificación
     * @param doctorNotes    notas del doctor (opcional)
     * @param appointmentId  id de la cita en MySQL
     */
    public Prescription(String patientName, String medication, String dosage, String doctorNotes, Long appointmentId) {
        this.patientName = patientName;
        this.medication = medication;
        this.dosage = dosage;
        this.doctorNotes = doctorNotes;
        this.appointmentId = appointmentId;
    }

    /** @return identificador MongoDB del documento */
    public String getId() {
        return id;
    }

    /** @param id identificador a asignar */
    public void setId(String id) {
        this.id = id;
    }

    /** @return nombre del paciente */
    public String getPatientName() {
        return patientName;
    }

    /** @param patientName nombre a asignar */
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    /** @return id de la cita asociada en MySQL */
    public Long getAppointmentId() {
        return appointmentId;
    }

    /** @param appointmentId id de la cita a asignar */
    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    /** @return nombre del medicamento */
    public String getMedication() {
        return medication;
    }

    /** @param medication medicamento a asignar */
    public void setMedication(String medication) {
        this.medication = medication;
    }

    /** @return indicaciones de dosificación */
    public String getDosage() {
        return dosage;
    }

    /** @param dosage dosificación a asignar */
    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    /** @return notas del doctor (puede ser null) */
    public String getDoctorNotes() {
        return doctorNotes;
    }

    /** @param doctorNotes notas a asignar (opcional) */
    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

}
