package com.banco.bff.cajero;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.bff.web.dto.ErrorDTO;
import com.banco.bff.web.dto.RetiroRequestDTO;
import com.banco.bff.web.dto.RetiroResponseDTO;
import com.banco.bff.web.dto.SaldoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cajero/cuentas")
public class CajeroController {
    private final CuentaInteresRepository cuentaInteresRepository;

    public CajeroController(CuentaInteresRepository cuentaInteresRepository) {
        this.cuentaInteresRepository = cuentaInteresRepository;
    }

    @GetMapping("/{cuentaId}/saldo")
    public ResponseEntity<?> saldo(@PathVariable Long cuentaId) {
        return cuentaInteresRepository.findById(cuentaId)
                .map(cuenta -> ResponseEntity.ok((Object) new SaldoDTO(cuenta.getCuentaId(), cuenta.getSaldo())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body((Object) new ErrorDTO("Cuenta no encontrada")));
    }

    @GetMapping("/{cuentaId}/validar")
    public ResponseEntity<Void> validar(@PathVariable Long cuentaId) {
        boolean existe = cuentaInteresRepository.existsById(cuentaId);
        return existe ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{cuentaId}/retiro")
    public ResponseEntity<?> retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequestDTO request){
        if(request.monto() == null || request.monto() <= 0){
            return ResponseEntity.badRequest().body(new ErrorDTO("El monto a retirar debe ser mayor a cero"));
        }

        var cuentaOpt = cuentaInteresRepository.findById(cuentaId);
        if(cuentaOpt.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Cuenta no encontrada"));
        }


        CuentaInteres cuenta = cuentaOpt.get();
        if(cuenta.getSaldo() == null || cuenta.getSaldo() < request.monto()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO("Saldo insuficiente para realizar el retiro"));
        }

        double nuevoSaldo = cuenta.getSaldo() - request.monto();
        cuenta.setSaldo(nuevoSaldo);
        cuentaInteresRepository.save(cuenta);
        return ResponseEntity.ok().body(new RetiroResponseDTO(cuentaId, request.monto(), nuevoSaldo));
    }
}
