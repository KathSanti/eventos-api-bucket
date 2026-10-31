package com.duoc.eventos_api_bucket.service;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.eventos_api_bucket.dto.ArchivoResumen;
import com.duoc.eventos_api_bucket.exception.RecursoNoEncontradoException;
import com.duoc.eventos_api_bucket.model.Cliente;
import com.duoc.eventos_api_bucket.model.Compra;
import com.duoc.eventos_api_bucket.model.Rol;
import com.duoc.eventos_api_bucket.repository.CompraRepository;

import lombok.RequiredArgsConstructor;

/**
 * Genera el resumen de compra (archivo .txt) y valida quién puede acceder a él.
 */
@Service
@RequiredArgsConstructor
public class ResumenCompraService {

    public static final String CONTENT_TYPE = "text/plain; charset=UTF-8";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final CompraRepository compraRepository;

    /**
     * Devuelve la compra SOLO si el usuario es su dueño o es ADMIN.
     *
     * Si no lo es, responde lo mismo que si no existiera (404, no 403): así un usuario
     * no puede descubrir qué números de resumen existen probando 1, 2, 3...
     * Esta es la defensa contra IDOR (acceder a objetos de otros cambiando el id en la URL).
     */
    @Transactional(readOnly = true)
    public Compra obtenerCompraAutorizada(Long numero, Cliente usuario) {
        Compra compra = compraRepository.findConDetalleById(numero)
                .orElseThrow(() -> noExiste(numero));

        boolean esDueno = compra.getCliente().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRol() == Rol.ADMIN;
        if (!esDueno && !esAdmin) {
            throw noExiste(numero);
        }
        return compra;
    }

    /** Genera el archivo físico del resumen (lo que el usuario guarda en su computador). */
    @Transactional(readOnly = true)
    public ArchivoResumen generar(Long numero, Cliente usuario) {
        Compra compra = obtenerCompraAutorizada(numero, usuario);
        String texto = armarTexto(compra);
        return new ArchivoResumen(nombreArchivo(numero), CONTENT_TYPE, texto.getBytes(StandardCharsets.UTF_8));
    }

    public String nombreArchivo(Long numero) {
        return "resumen-compra-" + numero + ".txt";
    }

    private String armarTexto(Compra compra) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(Locale.of("es", "CL"));
        String linea = "=".repeat(44);

        // Solo los datos necesarios: nombre y email, nunca contraseña ni datos internos.
        return String.join("\n",
                linea,
                "        RESUMEN DE COMPRA N° " + compra.getId(),
                linea,
                "Fecha de compra : " + compra.getFechaCompra().format(FECHA_HORA),
                "Cliente         : " + compra.getCliente().getNombre(),
                "Email           : " + compra.getCliente().getEmail(),
                "",
                "Evento          : " + compra.getEvento().getNombre(),
                "Fecha evento    : " + compra.getEvento().getFecha().format(FECHA),
                "Cantidad        : " + compra.getCantidad(),
                "Precio unitario : " + clp.format(compra.getPrecioUnitario()),
                "TOTAL           : " + clp.format(compra.getPrecioTotal()),
                linea,
                "");
    }

    private RecursoNoEncontradoException noExiste(Long numero) {
        return new RecursoNoEncontradoException("No existe el resumen de compra N° " + numero);
    }
}
