package com.banco.clientes.dto;

import com.banco.clientes.model.PerfilCliente;
import jakarta.validation.constraints.NotNull;

public record CambioPerfilRequestDTO(@NotNull(message = "El perfil es obligatorio") PerfilCliente perfil) {
}
