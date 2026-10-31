package com.duoc.eventos_api_bucket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.duoc.eventos_api_bucket.model.Compra;

/**
 * Datos de una compra que se devuelven al cliente.
 * numeroResumen (= id de la compra) es el número con el que se genera, sube,
 * descarga, modifica o borra el resumen de compra, y el nombre de su carpeta en S3.
 */
public record CompraResponse(
        Long numeroResumen,
        String nombreEvento,
        LocalDate fechaEvento,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal precioTotal,
        LocalDateTime fechaCompra
) {

    public static CompraResponse desde(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getEvento().getNombre(),
                compra.getEvento().getFecha(),
                compra.getCantidad(),
                compra.getPrecioUnitario(),
                compra.getPrecioTotal(),
                compra.getFechaCompra());
    }
}
