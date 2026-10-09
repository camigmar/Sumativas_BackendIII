package com.banco.pagos.event;

public record TransaccionCompletadaEvent(Long pagoId, String tipo, Long cuentaOrigen, Long cuentaDestino,
                                         Double monto, String fecha) {
}
