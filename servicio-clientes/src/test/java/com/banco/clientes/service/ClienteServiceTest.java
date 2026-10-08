package com.banco.clientes.service;

import com.banco.clientes.dto.ClienteRequestDTO;
import com.banco.clientes.dto.ClienteResponseDTO;
import com.banco.clientes.exception.ClienteDuplicadoException;
import com.banco.clientes.exception.ClienteNoEncontradoException;
import com.banco.clientes.model.Cliente;
import com.banco.clientes.model.EstadoCliente;
import com.banco.clientes.model.PerfilCliente;
import com.banco.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void crear_casoExitosoAsignaFechaEstadoActivoYPerfilBasico() {
        ClienteRequestDTO request = new ClienteRequestDTO("12345678-9", "Ana", "Perez",
                "ana.perez@correo.cl", "+56911111111", "Av. Providencia 1234", null);
        when(clienteRepository.existsByRut("12345678-9")).thenReturn(false);
        when(clienteRepository.existsByEmail("ana.perez@correo.cl")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> {
            Cliente guardado = invocacion.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        ClienteResponseDTO respuesta = clienteService.crear(request);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        Cliente cliente = captor.getValue();
        assertThat(cliente.getFechaRegistro()).isNotNull();
        assertThat(cliente.getEstado()).isEqualTo(EstadoCliente.ACTIVO);
        assertThat(cliente.getPerfil()).isEqualTo(PerfilCliente.BASICO);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.rut()).isEqualTo("12345678-9");
        assertThat(respuesta.email()).isEqualTo("ana.perez@correo.cl");
    }

    @Test
    void crear_rutDuplicadoLanzaExcepcionYNoGuarda() {
        ClienteRequestDTO request = new ClienteRequestDTO("12345678-9", "Ana", "Perez",
                "ana.perez@correo.cl", null, null, PerfilCliente.PREMIUM);
        when(clienteRepository.existsByRut("12345678-9")).thenReturn(true);

        assertThrows(ClienteDuplicadoException.class, () -> clienteService.crear(request));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void obtener_clienteNoEncontradoLanzaExcepcion() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtener(999L));
    }

    @Test
    void cambiarPerfil_actualizaYGuardaElNuevoPerfil() {
        Cliente cliente = clienteExistente();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        ClienteResponseDTO respuesta = clienteService.cambiarPerfil(1L, PerfilCliente.PREMIUM);

        assertThat(respuesta.perfil()).isEqualTo(PerfilCliente.PREMIUM);
        assertThat(cliente.getPerfil()).isEqualTo(PerfilCliente.PREMIUM);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void darDeBaja_marcaInactivoSinBorrarLaFila() {
        Cliente cliente = clienteExistente();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.darDeBaja(1L);

        assertThat(cliente.getEstado()).isEqualTo(EstadoCliente.INACTIVO);
        verify(clienteRepository).save(cliente);
        verify(clienteRepository, never()).delete(any());
        verify(clienteRepository, never()).deleteById(any());
    }

    private Cliente clienteExistente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRut("12345678-9");
        cliente.setNombre("Ana");
        cliente.setApellido("Perez");
        cliente.setEmail("ana.perez@correo.cl");
        cliente.setFechaRegistro(LocalDateTime.now());
        cliente.setEstado(EstadoCliente.ACTIVO);
        cliente.setPerfil(PerfilCliente.BASICO);
        return cliente;
    }
}
