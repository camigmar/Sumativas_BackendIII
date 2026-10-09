package com.banco.pagos.dto;

// Cuerpo que espera servicio-cuentas en /debito y /credito.
public record MovimientoCuentaRequestDTO(Double monto) {
}
