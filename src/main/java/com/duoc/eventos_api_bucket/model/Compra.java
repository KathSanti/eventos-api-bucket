package com.duoc.eventos_api_bucket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // Cada atributo necesita su propia columna (antes repetía "precio_unitario").
    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    // LAZY: el cliente/evento se carga solo cuando se necesita.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Antes apuntaba a "cliente_id"; el evento tiene su propia llave foránea.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;
}
