package com.banco.pagos.controller;

import com.banco.pagos.dto.DepositoRequestDTO;
import com.banco.pagos.dto.PagoResponseDTO;
import com.banco.pagos.dto.RetiroRequestDTO;
import com.banco.pagos.dto.TransferenciaRequestDTO;
import com.banco.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/retiro")
    public ResponseEntity<PagoResponseDTO> retiro(@Valid @RequestBody RetiroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.retirar(request));
    }

    @PostMapping("/deposito")
    public ResponseEntity<PagoResponseDTO> deposito(@Valid @RequestBody DepositoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.depositar(request));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<PagoResponseDTO> transferencia(@Valid @RequestBody TransferenciaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.transferir(request));
    }

    @GetMapping("/{id}")
    public PagoResponseDTO obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    @GetMapping
    public List<PagoResponseDTO> listarPorCuenta(@RequestParam Long cuentaId) {
        return pagoService.listarPorCuenta(cuentaId);
    }
}
