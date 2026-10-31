package com.duoc.eventos_api_bucket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.eventos_api_bucket.model.Asset;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.service.AwsService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints del resumen de compra en AWS S3.
 *
 * A diferencia del ejemplo genérico de la materia (/s3/uploadFile?bucketname=...&filepath=...),
 * aquí el cliente NO elige el bucket ni la ruta: el bucket viene de la configuración y la
 * clave la arma el servidor a partir del número del resumen. Así nadie puede sobrescribir
 * o borrar archivos ajenos.
 */
@RestController
@RequestMapping("/api/compras/{numero}/resumen/s3")
@RequiredArgsConstructor
public class AwsController {

    private final AwsService awsService;

    /** Genera el resumen y lo sube a S3 en la carpeta {numero}/. */
    @PostMapping
    public ResponseEntity<Asset> subir(@PathVariable("numero") Long numero,
                                       @AuthenticationPrincipal Cliente usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(awsService.subirResumen(numero, usuario));
    }

    /** MODIFICAR: reemplaza el resumen en S3 por un archivo corregido (form-data, campo "file"). */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Asset> modificar(@PathVariable("numero") Long numero,
                                           @RequestPart("file") MultipartFile file,
                                           @AuthenticationPrincipal Cliente usuario) {
        return ResponseEntity.ok(awsService.reemplazarResumen(numero, file, usuario));
    }

    /** DESCARGAR: devuelve el archivo guardado en S3 como descarga. */
    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable("numero") Long numero,
                                            @AuthenticationPrincipal Cliente usuario) {
        return Descargas.comoDescarga(awsService.descargarResumen(numero, usuario));
    }

    /** BORRAR: elimina el resumen de S3. */
    @DeleteMapping
    public ResponseEntity<Void> eliminar(@PathVariable("numero") Long numero,
                                         @AuthenticationPrincipal Cliente usuario) {
        awsService.eliminarResumen(numero, usuario);
        return ResponseEntity.noContent().build();
    }
}
