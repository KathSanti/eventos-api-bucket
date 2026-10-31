package com.duoc.eventos_api_bucket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.eventos_api_bucket.dto.EventoResponse;
import com.duoc.eventos_api_bucket.dto.NuevoEventoRequest;
import com.duoc.eventos_api_bucket.exception.EventoYaExisteException;
import com.duoc.eventos_api_bucket.exception.RecursoNoEncontradoException;
import com.duoc.eventos_api_bucket.model.Evento;
import com.duoc.eventos_api_bucket.repository.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public List<EventoResponse> listarEventos() {
        return eventoRepository.findAll().stream()
                .map(EventoResponse::desde)
                .toList();
    }

    @Transactional
    public EventoResponse agregarEvento(NuevoEventoRequest datos) {
        String nombre = datos.nombre().trim();
        // En los records el getter es nombre(), no getNombre().
        if (eventoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new EventoYaExisteException("Ya existe un evento con el nombre '" + nombre + "'");
        }

        Evento evento = new Evento();
        evento.setNombre(nombre);
        evento.setFecha(datos.fecha());
        evento.setPrecio(datos.precio());
        evento.setDisponible(true);

        return EventoResponse.desde(eventoRepository.save(evento));
    }

    @Transactional(readOnly = true)
    public EventoResponse buscarPorNombre(String nombre) {
        return eventoRepository.findByNombreIgnoreCase(nombre.trim())
                .map(EventoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un evento con el nombre '" + nombre + "'"));
    }
}
