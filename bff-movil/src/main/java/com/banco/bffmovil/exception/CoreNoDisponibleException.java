package com.banco.bffmovil.exception;

public class CoreNoDisponibleException extends RuntimeException {

    public CoreNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
