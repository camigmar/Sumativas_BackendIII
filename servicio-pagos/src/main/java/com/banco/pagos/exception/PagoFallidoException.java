package com.banco.pagos.exception;

import com.banco.pagos.model.EstadoPago;
import org.springframework.http.HttpStatus;

// El pago ya quedo guardado con su estado final (FALLIDO, COMPENSADO o REQUIERE_REVISION);
// esta excepcion solo lleva al cliente el status y el motivo, con el id para consultarlo.
public class PagoFallidoException extends RuntimeException {

    private final Long pagoId;
    private final EstadoPago estado;
    private final HttpStatus status;

    public PagoFallidoException(Long pagoId, EstadoPago estado, HttpStatus status, String motivo) {
        super(motivo + " (pago " + pagoId + ", estado " + estado + ")");
        this.pagoId = pagoId;
        this.estado = estado;
        this.status = status;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
