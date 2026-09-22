package com.banco.bffcajero.controller;

import com.banco.bffcajero.dto.ErrorDTO;
import com.banco.bffcajero.dto.RetiroRequestDTO;
import com.banco.bffcajero.dto.RetiroResponseDTO;
import com.banco.bffcajero.exception.CoreErrorException;
import com.banco.bffcajero.service.CajeroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cajero/cuentas")
public class CajeroController {

    private final CajeroService cajeroService;

    public CajeroController(CajeroService cajeroService) {
        this.cajeroService = cajeroService;
    }

    @GetMapping("/{cuentaId}/saldo")
    public ResponseEntity<?> saldo(@PathVariable Long cuentaId) {
        return cajeroService.obtenerSaldo(cuentaId)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorDTO("Cuenta no encontrada")));
    }

    @GetMapping("/{cuentaId}/validar")
    public ResponseEntity<Void> validar(@PathVariable Long cuentaId) {
        boolean existe = cajeroService.existeCuenta(cuentaId);
        return existe ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{cuentaId}/retiro")
    public ResponseEntity<?> retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequestDTO request) {
        RetiroResponseDTO respuesta = cajeroService.retirar(cuentaId, request);
        return ResponseEntity.ok(respuesta);
    }

    @ExceptionHandler(CoreErrorException.class)
    public ResponseEntity<ErrorDTO> handleCoreError(CoreErrorException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorDTO(ex.getMessage()));
    }
}
