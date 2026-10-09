package com.banco.pagos.exception;

// Llego otra solicitud con la misma Idempotency-Key mientras la primera sigue en curso (pago PENDIENTE): 409.
// El cliente puede reintentar luego y recibira el resultado original.
public class SolicitudEnProcesoException extends RuntimeException {

    public SolicitudEnProcesoException(String message) {
        super(message);
    }
}
