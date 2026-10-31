package com.duoc.eventos_api_bucket.model;

/**
 * Roles del sistema. Es la ÚNICA fuente de verdad para los nombres de rol:
 * Spring Security los usa con el prefijo "ROLE_" (ROLE_CLIENTE, ROLE_ADMIN)
 * y en las reglas se escriben sin prefijo: hasRole("ADMIN").
 */
public enum Rol {
    CLIENTE,
    ADMIN
}
