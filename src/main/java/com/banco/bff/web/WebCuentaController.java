package com.banco.bff.web;
import com.banco.batch.model.MovimientoAnual;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.EstadoCuentaAnualRepository;
import com.banco.batch.repository.MovimientoAnualRepository;
import com.banco.bff.web.dto.CuentaWebDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/web/cuentas")
public class WebCuentaController {
    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final MovimientoAnualRepository movimientoAnualRepository;
    public WebCuentaController(CuentaInteresRepository cuentaInteresRepository,
                                EstadoCuentaAnualRepository estadoCuentaAnualRepository,
                                MovimientoAnualRepository movimientoAnualRepository) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.estadoCuentaAnualRepository = estadoCuentaAnualRepository;
        this.movimientoAnualRepository = movimientoAnualRepository;
    }
    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaWebDTO> detalle(@PathVariable Long cuentaId) {
        return cuentaInteresRepository.findById(cuentaId)
                .map(cuenta -> {
                    var estado = estadoCuentaAnualRepository.findById(cuentaId).orElse(null);
                    return ResponseEntity.ok(CuentaWebDTO.desde(cuenta, estado));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoAnual> movimientos(@PathVariable Long cuentaId) {
        return movimientoAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId);
    }
}
