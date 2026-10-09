package com.banco.pagos.dto;

import com.banco.pagos.model.EstadoPago;
import com.banco.pagos.model.Pago;
import com.banco.pagos.model.TipoPago;

import java.time.LocalDateTime;

public record PagoResponseDTO(
        Long id,
        TipoPago tipo,
        Long cuentaOrigen,
        Long cuentaDestino,
        Double monto,
        EstadoPago estado,
        String motivo,
        LocalDateTime fecha) {

    public static PagoResponseDTO desde(Pago pago) {
        return new PagoResponseDTO(
                pago.getId(),
                pago.getTipo(),
                pago.getCuentaOrigen(),
                pago.getCuentaDestino(),
                pago.getMonto(),
                pago.getEstado(),
                pago.getMotivo(),
                pago.getFecha());
    }
}
