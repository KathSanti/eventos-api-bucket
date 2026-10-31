package com.duoc.eventos_api_bucket.dto;

/**
 * Un archivo listo para devolverse como descarga: nombre, tipo y contenido.
 * Lo usan la generación local del resumen y la descarga desde S3.
 */
public record ArchivoResumen(
        String nombreArchivo,
        String contentType,
        byte[] contenido
) {}
