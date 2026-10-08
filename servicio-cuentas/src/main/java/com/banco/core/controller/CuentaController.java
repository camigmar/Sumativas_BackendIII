package com.banco.core.controller;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.model.MovimientoAnual;
import com.banco.core.dto.ErrorDTO;
import com.banco.core.dto.RetiroRequestDTO;
import com.banco.core.dto.RetiroResponseDTO;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import com.banco.core.service.CuentaService;
import com.banco.core.service.MovimientoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;

    public CuentaController(CuentaService cuentaService, MovimientoService movimientoService) {
        this.cuentaService = cuentaService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaInteres> obtener(@PathVariable Long cuentaId) {
        return cuentaService.obtenerCuenta(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/existe")
    public ResponseEntity<Void> existe(@PathVariable Long cuentaId) {
        return cuentaService.existeCuenta(cuentaId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{cuentaId}/estado-anual")
    public ResponseEntity<EstadoCuentaAnual> estadoAnual(@PathVariable Long cuentaId) {
        return cuentaService.obtenerEstadoAnual(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoAnual>> movimientos(@PathVariable Long cuentaId,
                                                               @RequestParam(required = false) Integer limite) {
        List<MovimientoAnual> movimientos = limite != null
                ? movimientoService.obtenerUltimosMovimientos(cuentaId, limite)
                : movimientoService.obtenerMovimientos(cuentaId);
        return ResponseEntity.ok(movimientos);
    }

    @PostMapping("/{cuentaId}/retiro")
    public ResponseEntity<RetiroResponseDTO> retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequestDTO request) {
        double nuevoSaldo = cuentaService.retirar(cuentaId, request.monto());
        return ResponseEntity.ok(new RetiroResponseDTO(cuentaId, request.monto(), nuevoSaldo));
    }

    @ExceptionHandler(MontoInvalidoException.class)
    public ResponseEntity<ErrorDTO> handleMontoInvalido(MontoInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ErrorDTO> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorDTO> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO(ex.getMessage()));
    }
}
