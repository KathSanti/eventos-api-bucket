package com.duoc.eventos_api_bucket.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los JWT de la API.
 *
 * - Firma HMAC-SHA256 con una clave secreta que viene de una variable de entorno.
 * - Payload mínimo: sub (email), iss, aud, iat, exp. Nada sensible: el payload
 *   solo está codificado en Base64URL, cualquiera puede leerlo.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMs;
    private final String issuer;
    private final String audience;

    // OJO: este @Value es el de Spring (org.springframework.beans...), no el de Lombok.
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expiracionMs,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.audience}") String audience) {
        // hmacShaKeyFor exige al menos 256 bits (32 bytes); si la clave es más corta lanza WeakKeyException.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiracionMs = expiracionMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generarToken(UserDetails usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(key)
                .compact();
    }

    /**
     * Devuelve el email (subject) si el token es válido.
     * Lanza JwtException si el token es falso, fue modificado, expiró,
     * no tiene firma (alg "none") o fue emitido por/para otro sistema.
     */
    public String extraerEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
