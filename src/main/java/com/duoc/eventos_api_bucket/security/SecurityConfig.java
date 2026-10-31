package com.duoc.eventos_api_bucket.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.duoc.eventos_api_bucket.service.JwtService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ManejadorErroresSeguridad manejadorErrores;
    private final AccesoAdminPorIp accesoAdminPorIp;
    private final String origenesPermitidos;

    public SecurityConfig(JwtService jwtService,
                          UserDetailsService userDetailsService,
                          ManejadorErroresSeguridad manejadorErrores,
                          AccesoAdminPorIp accesoAdminPorIp,
                          @Value("${app.cors.allowed-origins}") String origenesPermitidos) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.manejadorErrores = manejadorErrores;
        this.accesoAdminPorIp = accesoAdminPorIp;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService, accesoAdminPorIp, manejadorErrores);

        http
            // CSRF ataca cookies que el navegador envía solo; un header Bearer no se envía solo.
            // (Si algún día el token se guarda en una cookie, hay que reactivar CSRF.)
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 401 y 403 en JSON para los rechazos que ocurren en los filtros
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(manejadorErrores)
                .accessDeniedHandler(manejadorErrores))

            // Las reglas se evalúan de arriba hacia abajo: gana la PRIMERA que coincide.
            .authorizeHttpRequests(auth -> auth
                // Públicas
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/eventos", "/api/eventos/*").permitAll()
                .requestMatchers("/error").permitAll()

                // Solo admin: gestión de eventos
                .requestMatchers(HttpMethod.POST, "/api/eventos").hasRole("ADMIN")

                // Usuario logueado: su perfil y sus compras (antes que /api/clientes/**)
                .requestMatchers(HttpMethod.GET, "/api/clientes/me", "/api/clientes/me/compras").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/compras").hasRole("CLIENTE")

                // Resumen de compra (archivo local y S3). La URL solo exige estar logueado;
                // QUIÉN es dueño de la compra lo valida ResumenCompraService (dueño o ADMIN).
                .requestMatchers(HttpMethod.GET, "/api/compras/*/resumen").authenticated()
                .requestMatchers("/api/compras/*/resumen/s3").authenticated()

                // Solo admin: gestión de clientes
                .requestMatchers("/api/clientes/**").hasRole("ADMIN")

                // Denegar por defecto todo lo que no esté listado arriba
                .anyRequest().denyAll())

            // CLAVE: el filtro JWT corre DENTRO de la cadena de seguridad, antes de que
            // se decida si la petición pasa o no. Sin esta línea el token nunca se leía.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Qué orígenes (frontends) pueden llamar a la API desde el navegador. Cambia por entorno. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes); // nunca "*"
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Boot arma el AuthenticationManager con ClienteDetailsService + BCrypt.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
