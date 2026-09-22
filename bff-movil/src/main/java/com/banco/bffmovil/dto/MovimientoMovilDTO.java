package com.banco.bffmovil.dto;

import java.time.LocalDate;

public record MovimientoMovilDTO(LocalDate fecha, String transaccion, Double monto) {

    public static MovimientoMovilDTO desde(MovimientoDTO movimiento) {
        return new MovimientoMovilDTO(movimiento.fecha(), movimiento.transaccion(), movimiento.monto());
    }
}
