package com.banco.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

// Edad y tipo con las mismas reglas que aplica el batch de intereses (InteresProcessor).
public record AperturaCuentaRequestDTO(
        @NotNull(message = "El clienteId es obligatorio") Long clienteId,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La edad es obligatoria")
        @Min(value = 18, message = "La edad debe ser al menos 18")
        @Max(value = 100, message = "La edad no puede ser mayor a 100") Integer edad,
        @NotNull(message = "El tipo es obligatorio")
        @Pattern(regexp = "ahorro|prestamo|hipoteca", message = "El tipo debe ser ahorro, prestamo o hipoteca") String tipo,
        @NotNull(message = "El saldoInicial es obligatorio")
        @PositiveOrZero(message = "El saldoInicial no puede ser negativo") Double saldoInicial) {
}
