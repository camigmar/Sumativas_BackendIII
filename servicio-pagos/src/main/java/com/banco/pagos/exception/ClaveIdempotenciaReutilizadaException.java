package com.banco.pagos.exception;

// La Idempotency-Key ya se uso con una solicitud distinta (otro tipo, cuenta o monto): 422.
public class ClaveIdempotenciaReutilizadaException extends RuntimeException {

    public ClaveIdempotenciaReutilizadaException(String message) {
        super(message);
    }
}
