package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Servicio JWT (JJWT 0.12.6) responsable de generar, extraer y validar tokens.
 * El subject del token es el email del usuario; para admin es el username.
 * El claim {@code role} indica el rol del dueño ({@code admin|doctor|patient}).
 * Expiración: 7 días.
 */
@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    /** Secreto de firma HMAC-SHA; proviene de la property {@code jwt.secret}. */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Inyección por constructor de los repositorios usados al validar el rol del token.
     *
     * @param adminRepository   repositorio de admins
     * @param doctorRepository  repositorio de doctores
     * @param patientRepository repositorio de pacientes
     */
    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /** Clave HMAC-SHA derivada del secreto configurado (≥256 bits para HS256). */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un JWT firmado cuyo subject es el email (o username, para admin)
     * y cuyo claim {@code role} indica el rol del dueño.
     *
     * @param email subject del token (email, o username para admin)
     * @param role  rol del dueño: {@code admin|doctor|patient}
     * @return token JWT como string, expirando en 7 días
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1000L * 60 * 60 * 24 * 7);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Verifica la firma del token y devuelve su subject (email/username).
     *
     * @param token token JWT
     * @return subject del token
     * @throws Exception si el token es inválido, expirado o la firma no coincide
     */
    public String extractIdentifier(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Verifica la firma del token y devuelve el valor del claim {@code role}.
     *
     * @param token token JWT
     * @return rol del dueño: {@code admin}, {@code doctor} o {@code patient}
     * @throws Exception si el token es inválido, expirado o la firma no coincide
     */
    public String extractRole(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * Valida que el token pertenezca a un usuario existente del rol indicado.
     * Cualquier excepción (token corrupto, expirado, firma inválida) resulta en {@code false}.
     *
     * @param token token JWT
     * @param role  rol esperado: {@code admin}, {@code doctor} o {@code patient}
     * @return {@code true} si el subject existe en el repositorio del rol
     */
    public boolean validateToken(String token, String role) {
        try {
            String email = extractIdentifier(token);
            return switch (role) {
                case "admin" -> adminRepository.findByUsername(email) != null;
                case "doctor" -> doctorRepository.findByEmail(email) != null;
                case "patient" -> patientRepository.findByEmail(email) != null;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }
}
