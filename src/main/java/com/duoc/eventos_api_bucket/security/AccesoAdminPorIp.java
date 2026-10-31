package com.duoc.eventos_api_bucket.security;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * Decide desde qué IPs puede actuar un ADMINISTRADOR.
 *
 * Se configura con ADMIN_ALLOWED_IPS, separadas por coma, en formato IP o CIDR:
 *   ADMIN_ALLOWED_IPS=190.20.30.40/32,200.1.2.0/24
 * Si está vacía, no hay restricción (desarrollo y GitHub Actions).
 *
 * ¿Por qué en la aplicación y no en el Security Group de AWS?
 * El Security Group filtra por PUERTO, y clientes y admin usan el mismo puerto (80).
 * Solo la aplicación sabe si quien llama es admin.
 *
 * Se usa request.getRemoteAddr() (la IP real de la conexión) y NO el header
 * X-Forwarded-For: ese header lo puede inventar cualquiera. Si algún día se pone un
 * balanceador o un Nginx delante, hay que configurar server.forward-headers-strategy.
 */
@Component
public class AccesoAdminPorIp {

    private static final Logger log = LoggerFactory.getLogger(AccesoAdminPorIp.class);

    private final List<IpAddressMatcher> permitidas;

    public AccesoAdminPorIp(@Value("${app.admin.ips-permitidas:}") String ips) {
        // Una IP mal escrita lanza IllegalArgumentException al arrancar (fail fast).
        this.permitidas = Arrays.stream(ips.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();

        if (permitidas.isEmpty()) {
            log.warn("ADMIN_ALLOWED_IPS vacío: el administrador puede conectarse desde cualquier IP");
        } else {
            log.info("Administrador restringido a las IPs: {}", ips);
        }
    }

    public boolean permitida(String ip) {
        if (permitidas.isEmpty()) {
            return true;
        }
        return ip != null && permitidas.stream().anyMatch(m -> m.matches(ip));
    }
}
