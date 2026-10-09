package com.banco.pagos.exception;

import org.springframework.http.HttpStatus;

// servicio-cuentas rechazo la operacion con 409 (saldo insuficiente o cuenta cerrada); el mensaje es el de cuentas.
public class OperacionRechazadaException extends CuentasException {

    public OperacionRechazadaException(String message) {
        super(message, HttpStatus.CONFLICT, null);
    }
}
