package com.banco.bff.movil;

import java.time.LocalDate;

import com.banco.batch.model.MovimientoAnual;
    
public record MovimientoMovilDTO(LocalDate fecha, String transaccion, Double monto) {
    
    public static MovimientoMovilDTO fromEntity(MovimientoAnual movimiento) {
        return new MovimientoMovilDTO(movimiento.getFecha(), movimiento.getTransaccion(), movimiento.getMonto());
    }
}
