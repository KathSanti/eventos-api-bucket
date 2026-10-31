package com.duoc.eventos_api_bucket.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.eventos_api_bucket.dto.ArchivoResumen;
import com.duoc.eventos_api_bucket.exception.ArchivoInvalidoException;
import com.duoc.eventos_api_bucket.exception.RecursoNoEncontradoException;
import com.duoc.eventos_api_bucket.model.Asset;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.repository.S3Repository;

import lombok.RequiredArgsConstructor;

/**
 * Orquesta el flujo de cada operación:
 *   1) validar que el usuario puede acceder a ese resumen (dueño o ADMIN),
 *   2) armar la clave en S3 (la arma el SERVIDOR, nunca el cliente),
 *   3) delegar en S3Repository.
 */
@Service
@RequiredArgsConstructor
public class AwsServiceImpl implements AwsService {

    // 1 MB es de sobra para un resumen de texto. Nunca aceptar archivos de cualquier tamaño.
    private static final long TAMANO_MAXIMO = 1024 * 1024;

    private final S3Repository s3Repository;
    private final ResumenCompraService resumenCompraService;

    @Override
    public Asset subirResumen(Long numero, Cliente usuario) {
        // generar() ya valida que el usuario sea dueño o ADMIN
        ArchivoResumen resumen = resumenCompraService.generar(numero, usuario);
        return s3Repository.subir(claveDe(numero), resumen.contenido(), resumen.contentType());
    }

    @Override
    public Asset reemplazarResumen(Long numero, MultipartFile archivo, Cliente usuario) {
        resumenCompraService.obtenerCompraAutorizada(numero, usuario);
        String key = claveDe(numero);
        if (!s3Repository.existe(key)) {
            throw new RecursoNoEncontradoException(
                    "El resumen N° " + numero + " no está en S3. Súbelo primero con POST");
        }
        validarArchivo(archivo);
        try {
            // Misma clave = S3 reemplaza el archivo (con Versioning, la versión anterior queda guardada).
            return s3Repository.subir(key, archivo.getBytes(), ResumenCompraService.CONTENT_TYPE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido", e);
        }
    }

    @Override
    public ArchivoResumen descargarResumen(Long numero, Cliente usuario) {
        resumenCompraService.obtenerCompraAutorizada(numero, usuario);
        String key = claveDe(numero);
        if (!s3Repository.existe(key)) {
            throw new RecursoNoEncontradoException("El resumen N° " + numero + " no está en S3");
        }
        return new ArchivoResumen(
                resumenCompraService.nombreArchivo(numero),
                ResumenCompraService.CONTENT_TYPE,
                s3Repository.descargar(key));
    }

    @Override
    public void eliminarResumen(Long numero, Cliente usuario) {
        resumenCompraService.obtenerCompraAutorizada(numero, usuario);
        String key = claveDe(numero);
        // S3 no avisa si borras algo que no existe; lo revisamos para responder 404.
        if (!s3Repository.existe(key)) {
            throw new RecursoNoEncontradoException("El resumen N° " + numero + " no está en S3");
        }
        s3Repository.eliminar(key);
    }

    /**
     * En S3 no hay carpetas reales: todo es una clave. La "/" hace que la consola
     * la muestre como carpeta. Carpeta = número del resumen, como pide el caso.
     */
    private String claveDe(Long numero) {
        return numero + "/" + resumenCompraService.nombreArchivo(numero);
    }

    /** Solo .txt, no vacío y hasta 1 MB: nunca aceptar cualquier archivo que nos manden. */
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Debes adjuntar un archivo en el campo 'file'");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ArchivoInvalidoException("El archivo supera el máximo de 1 MB");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!tipo.startsWith("text/plain") && !nombre.endsWith(".txt")) {
            throw new ArchivoInvalidoException("Solo se aceptan archivos de texto (.txt)");
        }
    }
}
