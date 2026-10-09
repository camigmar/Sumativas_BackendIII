package com.banco.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaRequestDTO(
        @NotNull(message = "La cuenta de origen es obligatoria") Long cuentaOrigen,
        @NotNull(message = "La cuenta de destino es obligatoria") Long cuentaDestino,
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0") Double monto) {
}
