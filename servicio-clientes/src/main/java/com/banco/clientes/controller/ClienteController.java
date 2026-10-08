package com.banco.clientes.controller;

import com.banco.clientes.dto.CambioPerfilRequestDTO;
import com.banco.clientes.dto.ClienteRequestDTO;
import com.banco.clientes.dto.ClienteResponseDTO;
import com.banco.clientes.dto.ClienteUpdateRequestDTO;
import com.banco.clientes.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @GetMapping
    public List<ClienteResponseDTO> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @GetMapping("/rut/{rut}")
    public ClienteResponseDTO obtenerPorRut(@PathVariable String rut) {
        return clienteService.obtenerPorRut(rut);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequestDTO request) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/{id}/perfil")
    public ClienteResponseDTO cambiarPerfil(@PathVariable Long id, @Valid @RequestBody CambioPerfilRequestDTO request) {
        return clienteService.cambiarPerfil(id, request.perfil());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        clienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
