package com.banco.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Datos personales editables; el rut identifica al cliente y no se modifica.
public record ClienteUpdateRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido") String email,
        String telefono,
        String direccion) {
}
