package com.banco.auditoria.exception;

// Mensaje que no se puede auditar (JSON invalido o sin los campos esperados). El error handler de Kafka
// no lo reintenta: lo envia directo a <topico>.DLT para que no frene el consumo del resto.
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
