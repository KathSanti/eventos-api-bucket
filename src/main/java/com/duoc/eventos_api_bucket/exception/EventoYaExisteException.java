package com.duoc.eventos_api_bucket.exception;

public class EventoYaExisteException extends RuntimeException {
    public EventoYaExisteException(String mensaje) {
        super(mensaje);
    }
}
