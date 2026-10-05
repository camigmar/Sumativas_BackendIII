package com.banco.core.event;

public record RetiroRealizadoEvent(Long cuentaId, Double monto, Double nuevoSaldo, String fecha) {
}
