package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends CuentasException {

    public CuentaNoEncontradaException(String message) {
        super(message, HttpStatus.NOT_FOUND, null);
    }
}
