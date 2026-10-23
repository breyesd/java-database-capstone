package com.project.back_end.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * DTO de credenciales de login para doctor y paciente.
 * El campo {@code email} acepta también el alias {@code identifier}
 * en el request (convención de la guía del curso), de modo que ambos
 * formatos de body se deserializan correctamente.
 * No es una entidad: solo se usa en {@code @RequestBody}.
 */
public class Login {

    /**
     * Identificador de login: email para doctor/paciente.
     * Acepta también el nombre {@code identifier} en el JSON de entrada.
     */
    @JsonAlias("identifier")
    private String email;

    /** Contraseña proporcionada por el usuario. */
    private String password;

    /** Constructor vacío para la deserialización. */
    public Login() {
    }

    /** @return email de login */
    public String getEmail() {
        return email;
    }

    /** @param email email a asignar */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return contraseña de login */
    public String getPassword() {
        return password;
    }

    /** @param password contraseña a asignar */
    public void setPassword(String password) {
        this.password = password;
    }
}
