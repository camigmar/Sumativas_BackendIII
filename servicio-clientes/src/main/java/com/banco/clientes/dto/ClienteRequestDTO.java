package com.banco.clientes.dto;

import com.banco.clientes.model.PerfilCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Si no se indica perfil, el cliente se crea como BASICO.
public record ClienteRequestDTO(
        @NotBlank(message = "El rut es obligatorio") String rut,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido") String email,
        String telefono,
        String direccion,
        PerfilCliente perfil) {
}
