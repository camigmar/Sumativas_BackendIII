package com.banco.bff.movil;

import com.banco.core.service.CuentaService;
import com.banco.core.service.MovimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movil/cuentas")
public class MovilCuentaController {

    private static final int LIMITE_MOVIMIENTOS = 10;

    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;

    public MovilCuentaController(CuentaService cuentaService, MovimientoService movimientoService) {
        this.cuentaService = cuentaService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaMovilDTO> detalle(@PathVariable Long cuentaId) {
        return cuentaService.obtenerCuenta(cuentaId)
                .map(cuenta -> ResponseEntity.ok(CuentaMovilDTO.fromCuentaInteres(cuenta)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoMovilDTO>> movimientos(@PathVariable Long cuentaId) {
        if (!cuentaService.existeCuenta(cuentaId)) {
            return ResponseEntity.notFound().build();
        }

        List<MovimientoMovilDTO> movimientos = movimientoService.obtenerUltimosMovimientos(cuentaId, LIMITE_MOVIMIENTOS)
                .stream()
                .map(MovimientoMovilDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(movimientos);
    }
}
