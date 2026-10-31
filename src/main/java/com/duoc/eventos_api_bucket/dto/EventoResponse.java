package com.duoc.eventos_api_bucket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.duoc.eventos_api_bucket.model.Evento;

/** Datos públicos de un evento (antes EventoDto). No expone el id interno. */
public record EventoResponse(
        String nombre,
        LocalDate fecha,
        BigDecimal precio,
        boolean disponible
) {

    public static EventoResponse desde(Evento evento) {
        return new EventoResponse(
                evento.getNombre(),
                evento.getFecha(),
                evento.getPrecio(),
                evento.isDisponible());
    }
}
