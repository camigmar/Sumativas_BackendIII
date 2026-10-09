package com.banco.core.exception;

// servicio-clientes respondio un 4xx distinto de 404 (por ejemplo 403). No se reintenta ni cuenta
// como caida para el circuit breaker.
public class ClientesRespuestaInesperadaException extends RuntimeException {

    public ClientesRespuestaInesperadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
