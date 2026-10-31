package com.duoc.eventos_api_bucket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duoc.eventos_api_bucket.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    Optional<Evento> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
