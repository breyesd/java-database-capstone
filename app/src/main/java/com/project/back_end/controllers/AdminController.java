package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de administración: expone el login de admin
 * ({@code POST /admin}) que devuelve un JWT de rol admin.
 */
@RestController
@RequestMapping("${api.path}" + "admin")
public class AdminController {

    private final Service service;

    /**
     * Inyección por constructor del servicio compartido.
     *
     * @param service servicio con {@code validateAdmin}
     */
    public AdminController(Service service) {
        this.service = service;
    }

    /**
     * Login de admin. Body {@code Admin{username,password}}.
     *
     * @param admin credenciales del admin
     * @return 200 {@code {token}} / 401 / 500
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> adminLogin(@Valid @RequestBody Admin admin) {
        return service.validateAdmin(admin);
    }
}
