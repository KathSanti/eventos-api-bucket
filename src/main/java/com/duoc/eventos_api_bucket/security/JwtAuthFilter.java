package com.duoc.eventos_api_bucket.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.duoc.eventos_api_bucket.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee el header "Authorization: Bearer <token>", valida el JWT y, si es válido,
 * deja al usuario autenticado en el SecurityContext para esta petición.
 *
 * NO es @Component a propósito: si lo fuera, Spring Boot además lo registraría
 * como filtro normal del servidor, FUERA de la cadena de Spring Security.
 * Se crea en SecurityConfig y se agrega a la cadena con addFilterBefore(...).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccesoAdminPorIp accesoAdminPorIp;
    private final ManejadorErroresSeguridad manejadorErrores;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                         AccesoAdminPorIp accesoAdminPorIp, ManejadorErroresSeguridad manejadorErrores) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.accesoAdminPorIp = accesoAdminPorIp;
        this.manejadorErrores = manejadorErrores;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin token (o ya autenticado): seguimos. Si el endpoint es protegido,
        // Spring Security responderá 401 más adelante.
        if (header == null || !header.startsWith(PREFIJO)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO.length()).trim();

        try {
            String email = jwtService.extraerEmail(token);

            // Se recarga el usuario desde la BD en cada petición: así, si lo desactivan
            // o bloquean después del login, su token deja de servir de inmediato.
            UserDetails usuario = userDetailsService.loadUserByUsername(email);

            // Un token de ADMIN solo sirve desde las IPs permitidas: si se lo roban,
            // no se puede usar desde otra red.
            if (esAdmin(usuario) && !accesoAdminPorIp.permitida(request.getRemoteAddr())) {
                log.warn("Token de ADMIN rechazado desde la IP {}", request.getRemoteAddr());
                manejadorErrores.escribir(response, HttpStatus.FORBIDDEN,
                        "Acceso de administrador no permitido desde esta IP (" + request.getRemoteAddr() + ")");
                return; // se corta la petición aquí
            }

            if (usuario.isEnabled() && usuario.isAccountNonLocked()) {
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token inválido, expirado o de un usuario que ya no existe: la petición
            // sigue como NO autenticada. Nunca se escribe el token en el log (es una credencial).
            log.debug("JWT rechazado: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private boolean esAdmin(UserDetails usuario) {
        return usuario.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
