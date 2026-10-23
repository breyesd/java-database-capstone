package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de doctores: lista, alta/edición/borrado (solo admin),
 * login ({@code POST /doctor/login}), disponibilidad y filtros.
 * El token va como path variable final de la ruta (convención del frontend);
 * la autenticación/autorización (token + rol) las resuelve el filtro JWT
 * de Spring Security.
 */
@RestController
@RequestMapping("${api.path}" + "doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    /**
     * Inyección por constructor de servicios.
     *
     * @param doctorService servicio de doctores
     * @param service       servicio compartido (filtros de doctores)
     */
    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    /**
     * Disponibilidad de un doctor en una fecha: slots libres.
     * Ruta: {@code GET /doctor/availability/{user}/{doctorId}/{date}/{token}}.
     *
     * @param user     rol del solicitante (debe ser {@code admin}, {@code doctor} o {@code patient})
     * @param doctorId id del doctor
     * @param date     fecha {@code yyyy-MM-dd}
     * @param token    JWT del solicitante
     * @return 200 {@code {availability: [...]}} / 400 fecha inválida /
     *         401 si {@code user} no corresponde a un rol permitido
     */
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String token) {
        if (!"admin".equalsIgnoreCase(user) && !"doctor".equalsIgnoreCase(user)
                && !"patient".equalsIgnoreCase(user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid Token or session expired"));
        }
        try {
            return ResponseEntity.ok(Map.of("availability",
                    doctorService.getDoctorAvailability(doctorId, LocalDate.parse(date))));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format, expected yyyy-MM-dd"));
        }
    }

    /**
     * Lista todos los doctores (público). {@code GET /doctor}.
     *
     * @return 200 {@code {doctors: [...]}}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctors() {
        return ResponseEntity.ok(Map.of("doctors", doctorService.getDoctors()));
    }

    /**
     * Alta de doctor (solo admin). {@code POST /doctor/{token}}.
     *
     * @param doctor doctor validado por Bean Validation
     * @param token  JWT de admin
     * @return 200 éxito / 409 duplicado / 500
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(@Valid @RequestBody Doctor doctor,
                                                          @PathVariable String token) {
        return switch (doctorService.saveDoctor(doctor)) {
            case 1 -> ResponseEntity.ok(Map.of("message", "Doctor added to db"));
            case -1 -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Doctor already exists"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        };
    }

    /**
     * Login de doctor. {@code POST /doctor/login}; body
     * {@code Login{email,password}} (también acepta {@code identifier}).
     *
     * @param login credenciales del doctor
     * @return 200 {@code {token}} / 401 / 500
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@Valid @RequestBody Login login) {
        return doctorService.validateDoctor(login);
    }

    /**
     * Edición de doctor (solo admin). {@code PUT /doctor/{token}}.
     *
     * @param doctor doctor con datos actualizados
     * @param token  JWT de admin
     * @return 200 éxito / 404 no encontrado / 500
     */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(@Valid @RequestBody Doctor doctor,
                                                            @PathVariable String token) {
        return switch (doctorService.updateDoctor(doctor)) {
            case 1 -> ResponseEntity.ok(Map.of("message", "Doctor updated"));
            case -1 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Doctor not found"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        };
    }

    /**
     * Borrado de doctor (solo admin), elimina también sus citas.
     * {@code DELETE /doctor/{id}/{token}}.
     *
     * @param id    id del doctor
     * @param token JWT de admin
     * @return 200 éxito / 404 no encontrado / 500
     */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable long id,
                                                            @PathVariable String token) {
        return switch (doctorService.deleteDoctor(id)) {
            case 1 -> ResponseEntity.ok(Map.of("message", "Doctor deleted successfully"));
            case -1 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Doctor not found with id"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        };
    }

    /**
     * Filtro de doctores por nombre/hora/especialidad; el literal
     * {@code null} en cualquier posición significa "sin ese filtro".
     * {@code GET /doctor/filter/{name}/{time}/{speciality}}.
     *
     * @param name       fragmento del nombre o {@code null}
     * @param time       {@code AM}/{@code PM} o {@code null}
     * @param speciality especialidad o {@code null}
     * @return 200 {@code {doctors: [...]}}
     */
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(@PathVariable String name,
                                                      @PathVariable String time,
                                                      @PathVariable String speciality) {
        return ResponseEntity.ok(service.filterDoctor(name, speciality, time));
    }
}
