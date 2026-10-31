package com.duoc.eventos_api_bucket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un evento. Se valida aquí, en la "frontera" de la API,
 * para responder 400 con un mensaje claro antes de llegar a la BD.
 */
public record NuevoEventoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotNull @FutureOrPresent LocalDate fecha,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal precio
) {}
