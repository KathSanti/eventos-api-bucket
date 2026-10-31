package com.duoc.eventos_api_bucket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Rol;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    // La BD responde sí/no sin cargar todos los usuarios en memoria.
    boolean existsByRol(Rol rol);
}
