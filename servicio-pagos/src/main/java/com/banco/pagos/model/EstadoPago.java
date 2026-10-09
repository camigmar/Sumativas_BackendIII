package com.banco.pagos.model;

public enum EstadoPago {
    PENDIENTE,
    COMPLETADO,
    FALLIDO,
    // El credito de una transferencia fallo y el debito se devolvio a la cuenta origen.
    COMPENSADO,
    // El credito fallo y la devolucion tambien: el dinero quedo debitado y hay que corregirlo a mano.
    REQUIERE_REVISION
}
