package com.banco.bff.web;
import com.banco.batch.model.MovimientoAnual;
import com.banco.bff.web.dto.CuentaWebDTO;
import com.banco.core.service.CuentaService;
import com.banco.core.service.MovimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/web/cuentas")
public class WebCuentaController {
    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;
    public WebCuentaController(CuentaService cuentaService, MovimientoService movimientoService) {
        this.cuentaService = cuentaService;
        this.movimientoService = movimientoService;
    }
    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaWebDTO> detalle(@PathVariable Long cuentaId) {
        return cuentaService.obtenerCuenta(cuentaId)
                .map(cuenta -> {
                    var estado = cuentaService.obtenerEstadoAnual(cuentaId).orElse(null);
                    return ResponseEntity.ok(CuentaWebDTO.desde(cuenta, estado));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoAnual> movimientos(@PathVariable Long cuentaId) {
        return movimientoService.obtenerMovimientos(cuentaId);
    }
}
