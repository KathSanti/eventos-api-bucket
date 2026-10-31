package com.duoc.eventos_api_bucket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.eventos_api_bucket.dto.CompraRequest;
import com.duoc.eventos_api_bucket.dto.CompraResponse;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.service.CompraService;
import com.duoc.eventos_api_bucket.service.ResumenCompraService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;
    private final ResumenCompraService resumenCompraService;

    /** Solo CLIENTE (regla en SecurityConfig). Quién compra sale del token. */
    @PostMapping
    public ResponseEntity<CompraResponse> comprar(@AuthenticationPrincipal Cliente cliente,
                                                  @Valid @RequestBody CompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.comprar(cliente, request));
    }

    /**
     * Genera el resumen de compra como ARCHIVO FÍSICO descargable (.txt).
     * Solo el dueño de la compra o un ADMIN (lo valida ResumenCompraService).
     * En Postman: Send -> "Save response to file".
     */
    @GetMapping("/{numero}/resumen")
    public ResponseEntity<byte[]> descargarResumenGenerado(@PathVariable("numero") Long numero,
                                                           @AuthenticationPrincipal Cliente usuario) {
        return Descargas.comoDescarga(resumenCompraService.generar(numero, usuario));
    }
}
