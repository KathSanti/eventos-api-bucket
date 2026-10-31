package com.duoc.eventos_api_bucket.exception;

/** Un administrador intentó iniciar sesión desde una IP que no está en ADMIN_ALLOWED_IPS. */
public class AccesoAdminDenegadoException extends RuntimeException {
    public AccesoAdminDenegadoException(String mensaje) {
        super(mensaje);
    }
}
