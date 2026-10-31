package com.duoc.eventos_api_bucket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.duoc.eventos_api_bucket.model.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // @EntityGraph trae el evento en la misma consulta (evita una consulta extra por cada compra).
    @EntityGraph(attributePaths = "evento")
    List<Compra> findByClienteIdOrderByFechaCompraDesc(Long clienteId);

    // Para el resumen se necesitan el evento Y el cliente (para validar quién es el dueño).
    // Spring Data permite texto libre entre "find" y "By": "ConDetalle" solo hace legible el nombre.
    @EntityGraph(attributePaths = {"evento", "cliente"})
    Optional<Compra> findConDetalleById(Long id);
}
