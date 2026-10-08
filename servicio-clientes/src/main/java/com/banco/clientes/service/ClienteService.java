package com.banco.clientes.service;

import com.banco.clientes.dto.ClienteRequestDTO;
import com.banco.clientes.dto.ClienteResponseDTO;
import com.banco.clientes.dto.ClienteUpdateRequestDTO;
import com.banco.clientes.exception.ClienteDuplicadoException;
import com.banco.clientes.exception.ClienteNoEncontradoException;
import com.banco.clientes.model.Cliente;
import com.banco.clientes.model.EstadoCliente;
import com.banco.clientes.model.PerfilCliente;
import com.banco.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public ClienteResponseDTO crear(ClienteRequestDTO request) {
        if (clienteRepository.existsByRut(request.rut())) {
            throw new ClienteDuplicadoException("Ya existe un cliente con el rut " + request.rut());
        }
        if (clienteRepository.existsByEmail(request.email())) {
            throw new ClienteDuplicadoException("Ya existe un cliente con el email " + request.email());
        }

        Cliente cliente = new Cliente();
        cliente.setRut(request.rut());
        cliente.setNombre(request.nombre());
        cliente.setApellido(request.apellido());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());
        cliente.setFechaRegistro(LocalDateTime.now());
        cliente.setEstado(EstadoCliente.ACTIVO);
        cliente.setPerfil(request.perfil() != null ? request.perfil() : PerfilCliente.BASICO);

        return ClienteResponseDTO.desde(clienteRepository.save(cliente));
    }

    public List<ClienteResponseDTO> listar() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponseDTO::desde)
                .toList();
    }

    public ClienteResponseDTO obtener(Long id) {
        return ClienteResponseDTO.desde(buscar(id));
    }

    public ClienteResponseDTO obtenerPorRut(String rut) {
        return clienteRepository.findByRut(rut)
                .map(ClienteResponseDTO::desde)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado"));
    }

    public ClienteResponseDTO actualizar(Long id, ClienteUpdateRequestDTO request) {
        Cliente cliente = buscar(id);
        if (clienteRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ClienteDuplicadoException("Ya existe un cliente con el email " + request.email());
        }

        cliente.setNombre(request.nombre());
        cliente.setApellido(request.apellido());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());

        return ClienteResponseDTO.desde(clienteRepository.save(cliente));
    }

    public ClienteResponseDTO cambiarPerfil(Long id, PerfilCliente perfil) {
        Cliente cliente = buscar(id);
        cliente.setPerfil(perfil);
        return ClienteResponseDTO.desde(clienteRepository.save(cliente));
    }

    // Baja logica: el cliente queda INACTIVO y la fila se conserva.
    public void darDeBaja(Long id) {
        Cliente cliente = buscar(id);
        cliente.setEstado(EstadoCliente.INACTIVO);
        clienteRepository.save(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado"));
    }
}
