package com.duoc.eventos_api_bucket.service;

import org.springframework.web.multipart.MultipartFile;

import com.duoc.eventos_api_bucket.dto.ArchivoResumen;
import com.duoc.eventos_api_bucket.model.Asset;
import com.duoc.eventos_api_bucket.model.Cliente;

/**
 * Operaciones del resumen de compra en el bucket S3.
 * Todas reciben al usuario del token para validar que sea dueño de la compra (o ADMIN).
 */
public interface AwsService {

    /** Genera el resumen y lo sube a S3 en la carpeta {numero}/. */
    Asset subirResumen(Long numero, Cliente usuario);

    /** Reemplaza el resumen en S3 por un archivo corregido (modificar en caso de error). */
    Asset reemplazarResumen(Long numero, MultipartFile archivo, Cliente usuario);

    /** Descarga el resumen guardado en S3. */
    ArchivoResumen descargarResumen(Long numero, Cliente usuario);

    /** Borra el resumen de S3. */
    void eliminarResumen(Long numero, Cliente usuario);
}
