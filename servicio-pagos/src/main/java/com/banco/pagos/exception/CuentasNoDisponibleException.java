package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

public class CuentasNoDisponibleException extends CuentasException {

    public CuentasNoDisponibleException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
