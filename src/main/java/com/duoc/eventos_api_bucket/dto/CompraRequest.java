package com.duoc.eventos_api_bucket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que el cliente envía para comprar.
 * A propósito NO trae clienteId (sale del token) ni precio (sale de la BD):
 * todo lo que viene en el request puede ser manipulado por quien lo envía.
 */
public record CompraRequest(
        @NotBlank String nombreEvento,
        @NotNull @Min(1) @Max(10) Integer cantidad
) {}
