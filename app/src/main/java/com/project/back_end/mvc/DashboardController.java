package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller MVC (Thymeleaf) que sirve los dashboards de admin y doctor
 * tras validar el token JWT del rol correspondiente.
 */
@Controller
public class DashboardController {

    private final Service service;

    /**
     * Inyección por constructor del servicio compartido de validación.
     *
     * @param service servicio con {@code validateToken}
     */
    public DashboardController(Service service) {
        this.service = service;
    }

    /**
     * Sirve la vista del dashboard de admin si el token es válido para el rol admin;
     * en caso contrario redirige a la portada.
     *
     * @param token JWT de admin (path variable final de la ruta)
     * @return nombre de la vista {@code admin/adminDashboard} o {@code redirect:/}
     */
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, Object> errors = service.validateToken(token, "admin");
        if (errors.isEmpty()) {
            return "admin/adminDashboard";
        }
        return "redirect:/";
    }

    /**
     * Sirve la vista del dashboard de doctor si el token es válido para el rol doctor;
     * en caso contrario redirige a la portada.
     *
     * @param token JWT de doctor (path variable final de la ruta)
     * @return nombre de la vista {@code doctor/doctorDashboard} o {@code redirect:/}
     */
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, Object> errors = service.validateToken(token, "doctor");
        if (errors.isEmpty()) {
            return "doctor/doctorDashboard";
        }
        return "redirect:/";
    }
}
