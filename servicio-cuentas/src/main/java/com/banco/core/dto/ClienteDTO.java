package com.banco.core.dto;

// Solo los campos de la respuesta de servicio-clientes que necesita la apertura.
public record ClienteDTO(Long id, String estado) {
}
