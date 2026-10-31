package com.duoc.eventos_api_bucket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.eventos_api_bucket.dto.LoginRequest;
import com.duoc.eventos_api_bucket.dto.RegistroRequest;
import com.duoc.eventos_api_bucket.dto.TokenResponse;
import com.duoc.eventos_api_bucket.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<TokenResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        // getRemoteAddr(): IP real de la conexión (no un header que el cliente pueda inventar).
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }
}