package com.banco.pagos.event;

public enum TipoAlerta {
    // Pago FALLIDO porque servicio-cuentas lo rechazo (saldo insuficiente, cuenta inexistente o cerrada).
    OPERACION_RECHAZADA,
    // Transferencia COMPENSADO: el credito fallo y se devolvio el monto a la cuenta origen.
    COMPENSACION_EJECUTADA,
    // Transferencia que quedo debitada sin acreditar ni compensar.
    REQUIERE_REVISION,
    // Pago COMPLETADO con monto mayor o igual al umbral alertas.monto-elevado.
    MONTO_ELEVADO
}
