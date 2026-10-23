package com.project.back_end.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller MVC (Thymeleaf) que sirve los dashboards de admin y doctor.
 * La autenticación/autorización (token + rol) la resuelve el filtro JWT
 * de Spring Security; token malo → redirect a / vía AuthenticationEntryPoint.
 */
@Controller
public class DashboardController {

    /**
     * Sirve la vista del dashboard de admin.
     *
     * @param token JWT de admin (path variable final; validado por el filtro)
     * @return nombre de la vista {@code admin/adminDashboard}
     */
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        return "admin/adminDashboard";
    }

    /**
     * Sirve la vista del dashboard de doctor.
     *
     * @param token JWT de doctor (path variable final; validado por el filtro)
     * @return nombre de la vista {@code doctor/doctorDashboard}
     */
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        return "doctor/doctorDashboard";
    }
}
