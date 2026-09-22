package com.banco.bffcajero.exception;

public class CoreErrorException extends RuntimeException {

    private final int status;

    public CoreErrorException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
