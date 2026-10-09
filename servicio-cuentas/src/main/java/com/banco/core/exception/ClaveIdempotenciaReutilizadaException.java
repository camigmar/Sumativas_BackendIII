package com.banco.core.exception;

// La Idempotency-Key ya se uso para otro movimiento (otra cuenta, operacion o monto).
public class ClaveIdempotenciaReutilizadaException extends RuntimeException {

    public ClaveIdempotenciaReutilizadaException(String message) {
        super(message);
    }
}
