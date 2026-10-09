package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

// servicio-cuentas respondio un 4xx no previsto (401/403 tras renovar el token, 422 de clave reutilizada).
// No se reintenta ni cuenta como caida para el circuit breaker.
public class CuentasRespuestaInesperadaException extends CuentasException {

    public CuentasRespuestaInesperadaException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, cause);
    }
}
