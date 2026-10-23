package com.project.back_end.repo;

import com.project.back_end.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de {@link Admin}. Expone la búsqueda por username,
 * usada por el login de admin y por la validación de tokens de rol admin.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Busca un admin por su username (identificador de login y subject del JWT).
     *
     * @param username username del admin
     * @return el admin encontrado o {@code null} si no existe
     */
    Admin findByUsername(String username);
}
