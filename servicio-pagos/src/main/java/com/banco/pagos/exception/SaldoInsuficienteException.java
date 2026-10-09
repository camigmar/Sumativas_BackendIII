package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

public class SaldoInsuficienteException extends CuentasException {

    public SaldoInsuficienteException(String message) {
        super(message, HttpStatus.CONFLICT, null);
    }
}
