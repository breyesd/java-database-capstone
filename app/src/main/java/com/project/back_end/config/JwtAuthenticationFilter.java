package com.project.back_end.config;

import com.project.back_end.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro JWT stateless: extrae el token del último segmento de la ruta
 * (contrato del frontend del curso), valida firma/expiración/rol con
 * {@link TokenService} y embebe la autenticación {@code ROLE_*} en el
 * contexto. La autorización por URL la hace el SecurityFilterChain.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Inyección por constructor.
     *
     * @param tokenService            servicio JWT
     * @param authenticationEntryPoint manejador 401 (JSON o redirect HTML)
     */
    public JwtAuthenticationFilter(TokenService tokenService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.tokenService = tokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Valida el token del último segmento (si parece JWT) y autentica la request.
     *
     * @param request  request HTTP entrante
     * @param response respuesta
     * @param chain    cadena de filtros
     * @throws IOException      error de E/S
     * @throws ServletException error de servlet
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String[] segments = request.getRequestURI().substring(1).split("/");
        String last = segments.length > 0 ? segments[segments.length - 1] : "";
        boolean looksLikeJwt = last.chars().filter(c -> c == '.').count() == 2;

        if (looksLikeJwt && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String role = tokenService.extractRole(last);
                boolean roleKnown = List.of("admin", "doctor", "patient").contains(role);
                if (roleKnown && tokenService.validateToken(last, role)) {
                    String identifier = tokenService.extractIdentifier(last);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            identifier, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    authenticationEntryPoint.commence(request, response, null);
                    return;
                }
            } catch (Exception e) {
                authenticationEntryPoint.commence(request, response, null);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
