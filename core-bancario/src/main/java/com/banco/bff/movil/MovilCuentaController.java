package com.banco.bff.movil;

import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.MovimientoAnualRepository;
import com.banco.bff.movil.CuentaMovilDTO;
import com.banco.bff.movil.MovimientoMovilDTO;
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

    private final CuentaInteresRepository cuentaInteresRepository;
    private final MovimientoAnualRepository movimientoAnualRepository;

    public MovilCuentaController(CuentaInteresRepository cuentaInteresRepository, MovimientoAnualRepository movimientoAnualRepository) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.movimientoAnualRepository = movimientoAnualRepository;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaMovilDTO> detalle(@PathVariable Long cuentaId) {
        return cuentaInteresRepository.findById(cuentaId)
                .map(cuenta -> ResponseEntity.ok(CuentaMovilDTO.fromCuentaInteres(cuenta)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoMovilDTO>> movimientos(@PathVariable Long cuentaId) {
        if (!cuentaInteresRepository.existsById(cuentaId)) {
            return ResponseEntity.notFound().build();
        }

        List<MovimientoMovilDTO> movimientos = movimientoAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId)
                .stream()
                .limit(LIMITE_MOVIMIENTOS)
                .map(MovimientoMovilDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(movimientos);
    }
}
