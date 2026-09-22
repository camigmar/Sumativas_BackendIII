package com.banco.bff.cajero;

import com.banco.core.dto.ErrorDTO;
import com.banco.core.dto.RetiroRequestDTO;
import com.banco.core.dto.RetiroResponseDTO;
import com.banco.core.dto.SaldoDTO;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import com.banco.core.service.CuentaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cajero/cuentas")
public class CajeroController {

    private final CuentaService cuentaService;

    public CajeroController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/{cuentaId}/saldo")
    public ResponseEntity<?> saldo(@PathVariable Long cuentaId) {
        return cuentaService.obtenerCuenta(cuentaId)
                .<ResponseEntity<Object>>map(cuenta -> ResponseEntity.ok(new SaldoDTO(cuenta.getCuentaId(), cuenta.getSaldo())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorDTO("Cuenta no encontrada")));
    }

    @GetMapping("/{cuentaId}/validar")
    public ResponseEntity<Void> validar(@PathVariable Long cuentaId) {
        boolean existe = cuentaService.existeCuenta(cuentaId);
        return existe ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{cuentaId}/retiro")
    public ResponseEntity<?> retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequestDTO request) {
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
