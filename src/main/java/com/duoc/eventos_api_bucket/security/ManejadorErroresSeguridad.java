package com.duoc.eventos_api_bucket.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde los rechazos que ocurren en los FILTROS de Spring Security
 * (antes de llegar a los controllers, donde GlobalExceptionHandler no alcanza):
 *
 *  - 401 Unauthorized: "no sé quién eres" -> sin token, token inválido o expirado.
 *  - 403 Forbidden:    "sé quién eres, pero no tienes permiso".
 *
 * Usa el mismo formato JSON que GlobalExceptionHandler.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesión: el token no se envió, es inválido o expiró");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción");
    }

    // Visible en el paquete: también lo usa JwtAuthFilter para el rechazo por IP.
    void escribir(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Los mensajes son fijos (no vienen del usuario), por eso se puede armar el JSON a mano.
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\"}",
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje);
        response.getWriter().write(json);
    }
}
