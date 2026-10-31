package com.duoc.eventos_api_bucket.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.duoc.eventos_api_bucket.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return clienteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}