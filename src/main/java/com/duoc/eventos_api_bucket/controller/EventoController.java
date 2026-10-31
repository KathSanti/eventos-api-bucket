package com.duoc.eventos_api_bucket.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.eventos_api_bucket.dto.EventoResponse;
import com.duoc.eventos_api_bucket.dto.NuevoEventoRequest;
import com.duoc.eventos_api_bucket.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    /** Público: listado de eventos (nombre, fecha, precio, disponibilidad). */
    @GetMapping
    public List<EventoResponse> listarEventos() {
        return eventoService.listarEventos();
    }

    /** Público: detalle de un evento por su nombre. */
    @GetMapping("/{nombre}")
    public EventoResponse buscarPorNombre(@PathVariable("nombre") String nombre) {
        return eventoService.buscarPorNombre(nombre);
    }

    /**
     * Solo administradores. La regla también está en SecurityConfig (defensa en profundidad);
     * ambas deben usar el mismo nombre de rol que el enum Rol: "ADMIN".
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventoResponse> agregarEvento(@Valid @RequestBody NuevoEventoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.agregarEvento(datos));
    }
}
