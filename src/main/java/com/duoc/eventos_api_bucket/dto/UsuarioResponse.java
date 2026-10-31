package com.duoc.eventos_api_bucket.dto;

import com.duoc.eventos_api_bucket.model.Rol;

/** Perfil del usuario autenticado. Nunca incluye la contraseña ni datos internos. */
public record UsuarioResponse(
        String nombre,
        String email,
        Rol rol
) {}
