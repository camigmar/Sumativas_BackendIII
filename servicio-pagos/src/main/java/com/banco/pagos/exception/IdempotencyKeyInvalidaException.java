package com.banco.pagos.exception;

// Header Idempotency-Key con formato invalido (demasiado largo): 400.
public class IdempotencyKeyInvalidaException extends RuntimeException {

    public IdempotencyKeyInvalidaException(String message) {
        super(message);
    }
}
