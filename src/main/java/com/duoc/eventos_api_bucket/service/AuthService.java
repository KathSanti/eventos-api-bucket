package com.duoc.eventos_api_bucket.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.eventos_api_bucket.dto.LoginRequest;
import com.duoc.eventos_api_bucket.dto.RegistroRequest;
import com.duoc.eventos_api_bucket.dto.TokenResponse;
import com.duoc.eventos_api_bucket.exception.AccesoAdminDenegadoException;
import com.duoc.eventos_api_bucket.exception.EmailYaRegistradoException;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Rol;
import com.duoc.eventos_api_bucket.repository.ClienteRepository;
import com.duoc.eventos_api_bucket.security.AccesoAdminPorIp;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccesoAdminPorIp accesoAdminPorIp;

    @Transactional
    public TokenResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (clienteRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException("El email ya está registrado");
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre().trim());
        cliente.setEmail(email);
        cliente.setPassword(passwordEncoder.encode(request.password()));
        cliente.setRol(Rol.CLIENTE); // el registro público SIEMPRE crea clientes, nunca admins
        clienteRepository.save(cliente);
        return new TokenResponse(jwtService.generarToken(cliente));
    }

    /**
     * Sin @Transactional a propósito: si fuera una sola transacción, al relanzar
     * la excepción también se desharía el contador de intentos fallidos.
     * Cada método de ClienteService abre su propia transacción.
     */
    public TokenResponse login(LoginRequest request, String ip) {
        String email = normalizarEmail(request.email());
        try {
            // Si la cuenta está bloqueada, Spring lanza LockedException ANTES de revisar la contraseña.
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            clienteService.reiniciarIntentos(email);

            // La contraseña era correcta, pero un ADMIN solo recibe token desde una IP permitida.
            Cliente usuario = (Cliente) auth.getPrincipal();
            if (usuario.getRol() == Rol.ADMIN && !accesoAdminPorIp.permitida(ip)) {
                throw new AccesoAdminDenegadoException(
                        "El administrador solo puede iniciar sesión desde una red autorizada (tu IP: " + ip + ")");
            }
            return new TokenResponse(jwtService.generarToken(usuario));
        } catch (BadCredentialsException e) {
            clienteService.registrarIntentoFallido(email);
            throw e; // se relanza para que GlobalExceptionHandler responda 401
        }
    }

    // "Ana@Mail.com " y "ana@mail.com" deben ser la misma cuenta.
    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
