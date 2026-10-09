package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

public class MontoInvalidoException extends CuentasException {

    public MontoInvalidoException(String message) {
        super(message, HttpStatus.BAD_REQUEST, null);
    }
}
