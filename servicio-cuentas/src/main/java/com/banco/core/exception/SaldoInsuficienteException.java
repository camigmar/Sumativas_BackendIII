package com.banco.core.exception;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String message) {
        super(message);
    }
}
