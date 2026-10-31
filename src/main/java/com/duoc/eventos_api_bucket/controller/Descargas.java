package com.duoc.eventos_api_bucket.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.duoc.eventos_api_bucket.dto.ArchivoResumen;

/**
 * Arma la respuesta HTTP de una descarga de archivo.
 *
 * El header "Content-Disposition: attachment" le dice al navegador (o a Postman con
 * "Save response to file") que guarde la respuesta como archivo en vez de mostrarla.
 */
final class Descargas {

    private Descargas() {
    }

    static ResponseEntity<byte[]> comoDescarga(ArchivoResumen archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .contentLength(archivo.contenido().length)
                .body(archivo.contenido());
    }
}
