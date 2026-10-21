package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entidad JPA que representa un paciente registrado en la clínica.
 * Mapea la tabla {@code patients}; sus datos de contacto se validan
 * con Bean Validation antes de persistir.
 */
@Entity
public class Patient {
// @Entity annotation:
//    - Marks the class as a JPA entity, meaning it represents a table in the database.
//    - Required for persistence frameworks (e.g., Hibernate) to map the class to a database table.

// 1. 'id' field:
//    - Type: private Long
//    - Description:
//      - Represents the unique identifier for each patient.
//      - The @Id annotation marks it as the primary key.
//      - The @GeneratedValue(strategy = GenerationType.IDENTITY) annotation auto-generates the ID value when a new record is inserted into the database.

// 2. 'name' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's full name.
//      - The @NotNull annotation ensures that the patient's name is required.
//      - The @Size(min = 3, max = 100) annotation ensures that the name length is between 3 and 100 characters. 
//      - Provides validation for correct input and user experience.


// 3. 'email' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's email address.
//      - The @NotNull annotation ensures that an email address must be provided.
//      - The @Email annotation validates that the email address follows a valid email format (e.g., patient@example.com).

// 4. 'password' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's password for login authentication.
//      - The @NotNull annotation ensures that a password must be provided.
//      - The @Size(min = 6) annotation ensures that the password must be at least 6 characters long.

// 5. 'phone' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's phone number.
//      - The @NotNull annotation ensures that a phone number must be provided.
//      - The @Pattern(regexp = "^[0-9]{10}$") annotation validates that the phone number must be exactly 10 digits long.

// 6. 'address' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's address.
//      - The @NotNull annotation ensures that the address must be provided.
//      - The @Size(max = 255) annotation ensures that the address does not exceed 255 characters in length, providing validation for the address input.


// 7. Getters and Setters:
//    - Standard getter and setter methods are provided for all fields: id, name, email, password, phone, and address.
//    - These methods allow access and modification of the fields of the Patient class.

    /** Identificador autoincremental (primary key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo; obligatorio, entre 3 y 100 caracteres. */
    @NotNull
    @Size(min = 3, max = 100)
    private String name;

    /** Correo electrónico de login; obligatorio y con formato válido (también usado como subject del JWT). */
    @NotNull
    @Email
    private String email;

    /** Credencial de acceso; obligatoria, mínimo 6 caracteres. */
    @NotNull
    @Size(min = 6)
    private String password;

    /** Teléfono de exactamente 10 dígitos; el registro rechaza teléfonos duplicados en la capa servicio. */
    @NotNull
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    /** Dirección postal; obligatoria, máximo 255 caracteres. */
    @NotNull
    @Size(max = 255)
    private String address;

    /** Constructor vacío requerido por JPA. */
    public Patient() {
    }

    /**
     * Constructor para crear un paciente con todos sus datos.
     *
     * @param name    nombre completo
     * @param email   correo electrónico de login
     * @param password contraseña (mínimo 6 caracteres)
     * @param phone   teléfono de 10 dígitos
     * @param address dirección (máximo 255 caracteres)
     */
    public Patient(String name, String email, String password, String phone, String address) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
    }

    /** @return identificador del paciente */
    public Long getId() {
        return id;
    }

    /** @param id identificador a asignar */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return nombre del paciente */
    public String getName() {
        return name;
    }

    /** @param name nombre a asignar */
    public void setName(String name) {
        this.name = name;
    }

    /** @return correo electrónico */
    public String getEmail() {
        return email;
    }

    /** @param email correo a asignar */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return contraseña (uso interno en autenticación) */
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

    /** @return dirección postal */
    public String getAddress() {
        return address;
    }

    /** @param address dirección a asignar */
    public void setAddress(String address) {
        this.address = address;
    }

}
