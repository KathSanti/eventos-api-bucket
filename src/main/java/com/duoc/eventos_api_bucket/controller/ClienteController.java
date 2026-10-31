package com.duoc.eventos_api_bucket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.eventos_api_bucket.dto.ClienteAdminResponse;
import com.duoc.eventos_api_bucket.dto.CompraResponse;
import com.duoc.eventos_api_bucket.dto.UsuarioResponse;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.service.ClienteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // ---------- Usuario autenticado (el cliente sale del token, nunca de un id en la URL) ----------

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> miPerfil(@AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(clienteService.obtenerPerfil(cliente));
    }

    @GetMapping("/me/compras")
    public ResponseEntity<List<CompraResponse>> misCompras(@AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(clienteService.obtenerHistorial(cliente));
    }

    // ---------- Solo administrador ----------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ClienteAdminResponse>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<Void> desbloquear(@PathVariable("id") Long id) {
        clienteService.desbloquearCliente(id);
        return ResponseEntity.noContent().build();
    }
}
