package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa un doctor del sistema.
 * Mapea la tabla {@code doctors} y agrupa sus datos de contacto,
 * credenciales de acceso y la colección de slots de disponibilidad.
 */
@Entity
public class Doctor {

// @Entity annotation:
//    - Marks the class as a JPA entity, meaning it represents a table in the database.
//    - Required for persistence frameworks (e.g., Hibernate) to map the class to a database table.

// 1. 'id' field:
//    - Type: private Long
//    - Description:
//      - Represents the unique identifier for each doctor.
//      - The @Id annotation marks it as the primary key.
//      - The @GeneratedValue(strategy = GenerationType.IDENTITY) annotation auto-generates the ID value when a new record is inserted into the database.

// 2. 'name' field:
//    - Type: private String
//    - Description:
//      - Represents the doctor's name.
//      - The @NotNull annotation ensures that the doctor's name is required.
//      - The @Size(min = 3, max = 100) annotation ensures that the name length is between 3 and 100 characters. 
//      - Provides validation for correct input and user experience.


// 3. 'specialty' field:
//    - Type: private String
//    - Description:
//      - Represents the medical specialty of the doctor.
//      - The @NotNull annotation ensures that a specialty must be provided.
//      - The @Size(min = 3, max = 50) annotation ensures that the specialty name is between 3 and 50 characters long.

// 4. 'email' field:
//    - Type: private String
//    - Description:
//      - Represents the doctor's email address.
//      - The @NotNull annotation ensures that an email address is required.
//      - The @Email annotation validates that the email address follows a valid email format (e.g., doctor@example.com).

// 5. 'password' field:
//    - Type: private String
//    - Description:
//      - Represents the doctor's password for login authentication.
//      - The @NotNull annotation ensures that a password must be provided.
//      - The @Size(min = 6) annotation ensures that the password must be at least 6 characters long.
//      - The @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) annotation ensures that the password is not serialized in the response (hidden from the frontend).

// 6. 'phone' field:
//    - Type: private String
//    - Description:
//      - Represents the doctor's phone number.
//      - The @NotNull annotation ensures that a phone number must be provided.
//      - The @Pattern(regexp = "^[0-9]{10}$") annotation validates that the phone number must be exactly 10 digits long.

// 7. 'availableTimes' field:
//    - Type: private List<String>
//    - Description:
//      - Represents the available times for the doctor in a list of time slots.
//      - Each time slot is represented as a string (e.g., "09:00-10:00", "10:00-11:00").
//      - The @ElementCollection annotation ensures that the list of time slots is stored as a separate collection in the database.

// 8. Getters and Setters:
//    - Standard getter and setter methods are provided for all fields: id, name, specialty, email, password, phone, and availableTimes.

    /** Identificador autoincremental (primary key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo del doctor; obligatorio, entre 3 y 100 caracteres. */
    @NotNull
    @Size(min = 3, max = 100)
    private String name;

    /** Especialidad médica; obligatoria, entre 3 y 50 caracteres. */
    @NotNull
    @Size(min = 3, max = 50)
    private String specialty;

    /** Correo electrónico de contacto; obligatorio y con formato de email válido. */
    @NotNull
    @Email
    private String email;

    /** Credencial de acceso; mínima 6 caracteres y nunca serializada en respuestas JSON. */
    @NotNull
    @Size(min = 6)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Teléfono de 10 dígitos; se valida el formato exacto en la anotación. */
    @NotNull
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    /**
     * Slots de disponibilidad en formato {@code "HH:mm-HH:mm"} (p. ej. {@code "09:00-10:00"}).
     * Persiste como colección separada {@code doctor_available_times}; una cita debe
     * coincidir con el inicio de uno de estos slots.
     */
    @ElementCollection
    @CollectionTable(name = "doctor_available_times", joinColumns = @JoinColumn(name = "doctor_id"))
    private List<String> availableTimes = new ArrayList<>();

    /** Constructor vacío requerido por JPA. */
    public Doctor() {
    }

    /**
     * Constructor para crear un doctor con todos sus datos de perfil.
     *
     * @param name          nombre completo
     * @param specialty     especialidad médica
     * @param email         correo electrónico de login/contacto
     * @param password      contraseña de acceso (mínimo 6 caracteres)
     * @param phone         teléfono de 10 dígitos
     * @param availableTimes lista de slots {@code "HH:mm-HH:mm"}
     */
    public Doctor(String name, String specialty, String email, String password, String phone, List<String> availableTimes) {
        this.name = name;
        this.specialty = specialty;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.availableTimes = availableTimes;
    }

    /** @return identificador del doctor */
    public Long getId() {
        return id;
    }

    /** @param id identificador a asignar */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return nombre del doctor */
    public String getName() {
        return name;
    }

    /** @param name nombre a asignar */
    public void setName(String name) {
        this.name = name;
    }

    /** @return especialidad médica */
    public String getSpecialty() {
        return specialty;
    }

    /** @param specialty especialidad a asignar */
    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    /** @return correo electrónico */
    public String getEmail() {
        return email;
    }

    /** @param email correo a asignar */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return contraseña (solo uso interno; WRITE_ONLY en JSON) */
    public String getPassword() {
        return password;
    }

    /** @param password contraseña a asignar */
    public void setPassword(String password) {
        this.password = password;
    }

    /** @return teléfono de 10 dígitos */
    public String getPhone() {
        return phone;
    }

    /** @param phone teléfono a asignar */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** @return lista de slots de disponibilidad {@code "HH:mm-HH:mm"} */
    public List<String> getAvailableTimes() {
        return availableTimes;
    }

    /** @param availableTimes slots de disponibilidad a asignar */
    public void setAvailableTimes(List<String> availableTimes) {
        this.availableTimes = availableTimes;
    }

}
