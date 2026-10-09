package com.banco.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Mantenimiento: solo datos descriptivos; saldo y clienteId no se modifican por aqui.
public record ActualizarCuentaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La edad es obligatoria")
        @Min(value = 18, message = "La edad debe ser al menos 18")
        @Max(value = 100, message = "La edad no puede ser mayor a 100") Integer edad,
        @NotNull(message = "El tipo es obligatorio")
        @Pattern(regexp = "ahorro|prestamo|hipoteca", message = "El tipo debe ser ahorro, prestamo o hipoteca") String tipo) {
}
