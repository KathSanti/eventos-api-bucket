package com.duoc.eventos_api_bucket.model;

import lombok.Builder;
import lombok.Value;

/**
 * Datos de un archivo guardado en S3 (no es una entidad de la BD).
 *
 * Aquí SÍ corresponde lombok.Value: marca la clase como inmutable
 * (campos private final, getters, equals/hashCode, sin setters).
 * Es distinto del @Value de Spring que inyecta propiedades (el de JwtService).
 */
@Value
@Builder
public class Asset {

    /** Clave completa en el bucket, por ejemplo "15/resumen-compra-15.txt". */
    String key;

    /** Carpeta dentro del bucket = número del resumen, por ejemplo "15/". */
    String carpeta;

    String nombreArchivo;

    String contentType;

    long tamanoBytes;
}
