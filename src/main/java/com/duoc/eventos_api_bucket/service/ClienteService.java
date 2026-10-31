package com.duoc.eventos_api_bucket.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.eventos_api_bucket.dto.ClienteAdminResponse;
import com.duoc.eventos_api_bucket.dto.CompraResponse;
import com.duoc.eventos_api_bucket.dto.UsuarioResponse;
import com.duoc.eventos_api_bucket.exception.RecursoNoEncontradoException;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.repository.ClienteRepository;
import com.duoc.eventos_api_bucket.repository.CompraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 10;

    private final ClienteRepository clienteRepository;
    private final CompraRepository compraRepository;

    // Perfil del usuario autenticado
    public UsuarioResponse obtenerPerfil(Cliente cliente) {
        return new UsuarioResponse(cliente.getNombre(), cliente.getEmail(), cliente.getRol());
    }

    // Historial de compras del usuario autenticado (el id sale del token, no del request)
    @Transactional(readOnly = true)
    public List<CompraResponse> obtenerHistorial(Cliente cliente) {
        return compraRepository.findByClienteIdOrderByFechaCompraDesc(cliente.getId())
                .stream()
                .map(CompraResponse::desde)
                .toList();
    }

    // Suma un intento fallido; al llegar a MAX_INTENTOS bloquea la cuenta por MINUTOS_BLOQUEO
    @Transactional
    public void registrarIntentoFallido(String email) {
        clienteRepository.findByEmail(email).ifPresent(cliente -> {
            cliente.setIntentosFallidos(cliente.getIntentosFallidos() + 1);
            if (cliente.getIntentosFallidos() >= MAX_INTENTOS) {
                cliente.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                cliente.setIntentosFallidos(0);
            }
            clienteRepository.save(cliente);
        });
    }

    // Después de un login correcto se limpian los intentos
    @Transactional
    public void reiniciarIntentos(String email) {
        clienteRepository.findByEmail(email).ifPresent(cliente -> {
            if (cliente.getIntentosFallidos() != 0 || cliente.getBloqueadoHasta() != null) {
                cliente.setIntentosFallidos(0);
                cliente.setBloqueadoHasta(null);
                clienteRepository.save(cliente);
            }
        });
    }

    // ---------- Solo administrador ----------

    @Transactional(readOnly = true)
    public List<ClienteAdminResponse> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(ClienteAdminResponse::desde)
                .toList();
    }

    @Transactional
    public void desbloquearCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        cliente.setIntentosFallidos(0);
        cliente.setBloqueadoHasta(null);
        clienteRepository.save(cliente);
    }
}
