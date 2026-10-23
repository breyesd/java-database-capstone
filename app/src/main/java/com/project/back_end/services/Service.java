package com.project.back_end.services;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio compartido de autenticación y reglas de negocio transversales.
 * Por ahora expone la validación de tokens usada por el MVC de dashboards;
 * el resto de métodos (logins, filtros, validaciones de cita/paciente)
 * se incorporan con sus specs correspondientes.
 */
@org.springframework.stereotype.Service
public class Service {

    private final TokenService tokenService;

    /**
     * Inyección por constructor de dependencias compartidas.
     *
     * @param tokenService generador/validador de JWT
     */
    public Service(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Valida un token JWT para un rol concreto.
     * Devuelve un mapa vacío si el token es válido; si no, un mapa con
     * {@code status=401} y un mensaje de error (formato esperado por los controllers).
     *
     * @param token token JWT a validar
     * @param role  rol esperado: {@code admin}, {@code doctor} o {@code patient}
     * @return mapa vacío si es válido; {@code {status:401, message}} si no
     */
    public Map<String, Object> validateToken(String token, String role) {
        Map<String, Object> map = new HashMap<>();
        boolean isValid = tokenService.validateToken(token, role);
        if (!isValid) {
            map.put("status", 401);
            map.put("message", "Invalid Token");
        }
        return map;
    }
}
