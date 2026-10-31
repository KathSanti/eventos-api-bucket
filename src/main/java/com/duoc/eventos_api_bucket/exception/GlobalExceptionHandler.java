package com.duoc.eventos_api_bucket.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import software.amazon.awssdk.core.exception.SdkException;

/**
 * Traduce excepciones a respuestas JSON con un formato único.
 *
 * OJO: este advice solo ve excepciones lanzadas DENTRO de los controllers.
 * Los rechazos por reglas de URL ocurren antes, en los filtros de Spring Security,
 * y los responde ManejadorErroresSeguridad con este mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Negocio ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EventoYaExisteException.class)
    public ResponseEntity<Map<String, Object>> handleYaExiste(EventoYaExisteException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleEmailYaRegistrado(EmailYaRegistradoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EventoNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleNoDisponible(EventoNoDisponibleException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Respaldo de la restricción UNIQUE de la BD (por ejemplo, dos registros simultáneos con el mismo email).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridad(DataIntegrityViolationException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
    }

    // ---------- Autenticación (login) ----------

    // Mismo mensaje para email inexistente y contraseña incorrecta: no revela qué emails existen.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(BadCredentialsException ex) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleBloqueada(LockedException ex) {
        return construirRespuesta(HttpStatus.LOCKED,
                "Cuenta bloqueada temporalmente por intentos fallidos. Intenta más tarde");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDesactivada(DisabledException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "La cuenta está desactivada");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAutenticacion(AuthenticationException ex) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, "No fue posible autenticar");
    }

    @ExceptionHandler(AccesoAdminDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handleAdminIp(AccesoAdminDenegadoException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ---------- Autorización (@PreAuthorize) ----------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(AccessDeniedException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "No tienes permisos para esta acción");
    }

    // ---------- Datos de entrada ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Formato inválido para el parámetro '" + ex.getName() + "'");
    }

    // ---------- Archivos y AWS S3 ----------

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleFaltaArchivo(MissingServletRequestPartException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST,
                "Falta el archivo: en Postman usa Body -> form-data, clave '" + ex.getRequestPartName() + "' de tipo File");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoGrande(MaxUploadSizeExceededException ex) {
        return construirRespuesta(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el tamaño máximo permitido");
    }

    // Errores al hablar con AWS: credenciales vencidas, bucket inexistente, sin permisos, sin red...
    // 502 = "el servidor de atrás (S3) falló", no es culpa del cliente ni un error de nuestro código.
    // io.awspring.cloud.s3.S3Exception: lo lanza S3Template al fallar una subida.
    // SdkException: cualquier error del SDK de AWS (incluye el S3Exception del SDK, p. ej. ExpiredToken).
    @ExceptionHandler({ io.awspring.cloud.s3.S3Exception.class, SdkException.class })
    public ResponseEntity<Map<String, Object>> handleS3(RuntimeException ex) {
        log.error("Error al comunicarse con AWS S3", ex);
        return construirRespuesta(HttpStatus.BAD_GATEWAY,
                "No se pudo acceder al almacenamiento S3. Revisa que las credenciales de AWS Academy estén vigentes y que el bucket exista");
    }

    // ---------- Cualquier otra cosa ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        // Las excepciones propias de Spring MVC (404, 405, 415...) ya traen su código HTTP correcto.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null) {
                return construirRespuesta(status, status.getReasonPhrase());
            }
        }
        // El detalle queda en el log del servidor; al cliente solo un mensaje genérico.
        log.error("Error no controlado", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
