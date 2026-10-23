package com.project.back_end.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security: chain stateless (CSRF off), matriz
 * RBAC por URL (spec 2026-09-28 §5), handlers 401/403 (JSON, o redirect
 * a / para navegadores), BCrypt y registro del filtro JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Codificador de contraseñas: BCrypt con coste 10.
     *
     * @return password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager global (usa {@code ClinicUserDetailsService}).
     *
     * @param configuration configuración de autenticación de Spring
     * @return authentication manager
     * @throws Exception error de configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 401: JSON {@code {"message":"Unauthorized"}} salvo que el navegador
     * pida HTML, en cuyo caso redirect a / (dashboards MVC del curso).
     *
     * @return entry point
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.AuthenticationException authException) -> {
            if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("text/html")) {
                response.sendRedirect("/");
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };
    }

    /**
     * 403: JSON {@code {"message":"Forbidden"}}; redirect a / si el
     * cliente prefiere HTML.
     *
     * @return access denied handler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("text/html")) {
                response.sendRedirect("/");
                return;
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Forbidden\"}");
        };
    }

    /**
     * Chain stateless con la matriz RBAC de la spec §5.
     *
     * @param http            configuración HttpSecurity
     * @param jwtFilter       filtro JWT propio
     * @param entryPoint      handler 401
     * @param deniedHandler   handler 403
     * @return security filter chain
     * @throws Exception error de configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler deniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // públicos
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/images/**", "/pages/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin", "/doctor/login", "/patient/login", "/patient").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctor", "/doctor/filter/**").permitAll()
                        // admin
                        .requestMatchers("/admin/**", "/adminDashboard/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/doctor/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/doctor/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/doctor/*/*").hasRole("ADMIN")
                        // doctor
                        .requestMatchers("/doctor/appointments/**", "/doctorDashboard/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/prescription/*").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/prescription/*/*").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/doctor/*").hasRole("DOCTOR")
                        // cross-rol
                        .requestMatchers(HttpMethod.GET, "/doctor/availability/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/patient/*/patient/*").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/patient/*/doctor/*").hasRole("DOCTOR")
                        // patient
                        .requestMatchers("/patient/filter/**").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/patient/*").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.POST, "/appointments/*").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.DELETE, "/appointments/*/*").hasRole("PATIENT")
                        // resto
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
