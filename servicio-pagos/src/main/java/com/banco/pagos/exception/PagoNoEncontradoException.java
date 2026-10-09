package com.banco.pagos.exception;

public class PagoNoEncontradoException extends RuntimeException {

    public PagoNoEncontradoException(String message) {
        super(message);
    }
}
