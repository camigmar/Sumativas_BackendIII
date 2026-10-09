package com.banco.pagos.event;

public record AlertaSeguridadEvent(String tipo, Long pagoId, Long cuentaId, Double monto, String detalle, String fecha) {
}
