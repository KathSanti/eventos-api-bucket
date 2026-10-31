package com.duoc.eventos_api_bucket.exception;

/** El archivo enviado para reemplazar un resumen no cumple las reglas (vacío, tipo o tamaño). */
public class ArchivoInvalidoException extends RuntimeException {
    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
