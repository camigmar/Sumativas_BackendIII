package com.banco.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositoRequestDTO(
        @NotNull(message = "La cuenta es obligatoria") Long cuentaId,
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0") Double monto) {
}
