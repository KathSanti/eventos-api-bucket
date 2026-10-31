package com.duoc.eventos_api_bucket.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.duoc.eventos_api_bucket.model.Evento;
import com.duoc.eventos_api_bucket.repository.EventoRepository;

/**
 * Carga eventos de ejemplo SOLO en el perfil "dev", para tener datos con qué
 * probar en Postman. En producción (@Profile distinto) este componente no existe.
 */
@Component
@Profile("dev")
public class DatosDevLoader implements ApplicationRunner {

    private final EventoRepository eventoRepository;

    public DatosDevLoader(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (eventoRepository.count() > 0) {
            return;
        }
        eventoRepository.save(crear("Concierto Rock Duoc", LocalDate.now().plusMonths(1), "25000"));
        eventoRepository.save(crear("Feria Tecnológica", LocalDate.now().plusMonths(2), "8000"));
        eventoRepository.save(crear("Obra de Teatro Clásico", LocalDate.now().plusWeeks(3), "15000"));
    }

    private Evento crear(String nombre, LocalDate fecha, String precio) {
        Evento evento = new Evento();
        evento.setNombre(nombre);
        evento.setFecha(fecha);
        evento.setPrecio(new BigDecimal(precio));
        evento.setDisponible(true);
        return evento;
    }
}
