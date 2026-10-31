package com.duoc.eventos_api_bucket.dto;

import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Rol;

/**
 * Vista de un cliente para el administrador. Incluye el id (lo necesita para
 * desbloquear) pero nunca el hash de la contraseña.
 */
public record ClienteAdminResponse(
        Long id,
        String nombre,
        String email,
        Rol rol,
        boolean activo,
        boolean bloqueado
) {

    public static ClienteAdminResponse desde(Cliente cliente) {
        return new ClienteAdminResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getRol(),
                cliente.isActivo(),
                !cliente.isAccountNonLocked());
    }
}
