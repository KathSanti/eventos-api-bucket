package com.duoc.eventos_api_bucket.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.eventos_api_bucket.dto.CompraRequest;
import com.duoc.eventos_api_bucket.dto.CompraResponse;
import com.duoc.eventos_api_bucket.exception.EventoNoDisponibleException;
import com.duoc.eventos_api_bucket.exception.RecursoNoEncontradoException;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Compra;
import com.duoc.eventos_api_bucket.model.Evento;
import com.duoc.eventos_api_bucket.repository.ClienteRepository;
import com.duoc.eventos_api_bucket.repository.CompraRepository;
import com.duoc.eventos_api_bucket.repository.EventoRepository;

import lombok.RequiredArgsConstructor;

/** Responsabilidad única: realizar compras. (El perfil y el historial viven en ClienteService.) */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public CompraResponse comprar(Cliente cliente, CompraRequest request) {
        String nombre = request.nombreEvento().trim();
        Evento evento = eventoRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un evento con el nombre '" + nombre + "'"));

        if (!evento.isDisponible() || evento.getFecha().isBefore(LocalDate.now())) {
            throw new EventoNoDisponibleException("El evento '" + evento.getNombre() + "' no está disponible para la venta");
        }

        // El precio SIEMPRE se toma de la BD, nunca del request.
        BigDecimal precioUnitario = evento.getPrecio();
        BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(request.cantidad()));

        Compra compra = new Compra();
        // El cliente viene del token. getReferenceById crea una referencia por id sin otra consulta.
        compra.setCliente(clienteRepository.getReferenceById(cliente.getId()));
        compra.setEvento(evento);
        compra.setCantidad(request.cantidad());
        compra.setPrecioUnitario(precioUnitario);
        compra.setPrecioTotal(precioTotal);
        compra.setFechaCompra(LocalDateTime.now());

        return CompraResponse.desde(compraRepository.save(compra));
    }
}
