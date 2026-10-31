package com.duoc.eventos_api_bucket.security;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Rol;
import com.duoc.eventos_api_bucket.repository.ClienteRepository;

/**
 * Crea el primer usuario ADMIN si todavía no existe ninguno.
 * El registro público solo crea CLIENTE; las credenciales del admin vienen
 * de variables de entorno (ADMIN_EMAIL, ADMIN_PASSWORD), nunca del código.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminNombre;

    // Las claves de @Value deben coincidir EXACTAMENTE con las de application.properties.
    public AdminBootstrap(ClienteRepository clienteRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email}") String adminEmail,
                          @Value("${app.admin.password}") String adminPassword,
                          @Value("${app.admin.nombre}") String adminNombre) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
        this.adminNombre = adminNombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (clienteRepository.existsByRol(Rol.ADMIN) || clienteRepository.existsByEmail(adminEmail)) {
            return;
        }

        Cliente admin = new Cliente();
        admin.setNombre(adminNombre);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMIN);
        clienteRepository.save(admin);

        log.info("Administrador inicial creado: {}", adminEmail);
    }
}
