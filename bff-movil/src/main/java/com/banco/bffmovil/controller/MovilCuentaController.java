package com.banco.bffmovil.controller;

import com.banco.bffmovil.dto.CuentaMovilDTO;
import com.banco.bffmovil.dto.MovimientoMovilDTO;
import com.banco.bffmovil.service.MovilCuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movil/cuentas")
public class MovilCuentaController {

    private final MovilCuentaService movilCuentaService;

    public MovilCuentaController(MovilCuentaService movilCuentaService) {
        this.movilCuentaService = movilCuentaService;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaMovilDTO> detalle(@PathVariable Long cuentaId) {
        return movilCuentaService.obtenerDetalle(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoMovilDTO>> movimientos(@PathVariable Long cuentaId) {
        if (!movilCuentaService.existeCuenta(cuentaId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(movilCuentaService.obtenerMovimientos(cuentaId));
    }
}
