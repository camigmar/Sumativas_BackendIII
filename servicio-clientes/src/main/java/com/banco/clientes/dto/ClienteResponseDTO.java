package com.banco.clientes.dto;

import com.banco.clientes.model.Cliente;
import com.banco.clientes.model.EstadoCliente;
import com.banco.clientes.model.PerfilCliente;

import java.time.LocalDateTime;

public record ClienteResponseDTO(
        Long id,
        String rut,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String direccion,
        LocalDateTime fechaRegistro,
        EstadoCliente estado,
        PerfilCliente perfil) {

    public static ClienteResponseDTO desde(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getRut(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getFechaRegistro(),
                cliente.getEstado(),
                cliente.getPerfil());
    }
}
