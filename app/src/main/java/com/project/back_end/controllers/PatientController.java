package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de pacientes: registro, login, datos del paciente
 * autenticado, historial de citas (paciente o doctor según {@code user})
 * y filtros de citas.
 */
@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    /**
     * Inyección por constructor de servicios.
     *
     * @param patientService servicio de pacientes
     * @param service        servicio compartido (validación de token y login)
     */
    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    /**
     * Datos del paciente autenticado. {@code GET /patient/{token}}.
     *
     * @param token JWT del paciente
     * @return 200 {@code {patient: {...}}} / 401
     */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("message", validation.getBody().get("message")));
        }
        return patientService.getPatientDetails(token);
    }

    /**
     * Registro de paciente (público). Rechaza email o teléfono duplicados.
     * {@code POST /patient}.
     *
     * @param patient paciente validado por Bean Validation
     * @return 200 éxito / 409 duplicado / 500
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@Valid @RequestBody Patient patient) {
        if (!service.validatePatient(patient)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Patient with email id or phone no already exist"));
        }
        if (patientService.createPatient(patient) == 1) {
            return ResponseEntity.ok(Map.of("message", "Signup successful"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error"));
    }

    /**
     * Login de paciente. {@code POST /patient/login}; body
     * {@code Login{email,password}} (también acepta {@code identifier}).
     *
     * @param login credenciales del paciente
     * @return 200 {@code {token}} / 401 / 500
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    /**
     * Historial de citas de un paciente. El token se valida contra el rol
     * {@code user} (patient u doctor); con rol patient el id debe coincidir
     * con el del token. {@code GET /patient/{id}/{user}/{token}}.
     *
     * @param id    id del paciente
     * @param user  rol del solicitante ({@code patient} o {@code doctor})
     * @param token JWT del solicitante
     * @return 200 {@code {appointments: [...]}} / 401
     */
    @GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(@PathVariable Long id,
                                                                     @PathVariable String user,
                                                                     @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("message", validation.getBody().get("message")));
        }
        return patientService.getPatientAppointment(id, token, user);
    }

    /**
     * Filtro de citas del paciente autenticado por condición
     * ({@code past}/{@code future}) y/o nombre de doctor; el literal
     * {@code null} significa "sin ese filtro".
     * {@code GET /patient/filter/{condition}/{name}/{token}}.
     *
     * @param condition condición o {@code null}
     * @param name      fragmento del nombre del doctor o {@code null}
     * @param token     JWT del paciente
     * @return 200 {@code {appointments: [...]}} / 400 / 401
     */
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(@PathVariable String condition,
                                                                        @PathVariable String name,
                                                                        @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(Map.of("message", validation.getBody().get("message")));
        }
        return service.filterPatient(condition, name, token);
    }
}
