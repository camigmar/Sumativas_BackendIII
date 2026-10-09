package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

// Base de los errores al operar sobre servicio-cuentas; cada subclase define el status HTTP con que se responde.
public abstract class CuentasException extends RuntimeException {

    private final HttpStatus status;

    protected CuentasException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
