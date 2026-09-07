package com.banco.bff.web;
import com.banco.batch.model.Transaccion;
import com.banco.batch.repository.TransaccionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/web/transacciones")
public class WebTransaccionController {
    private final TransaccionRepository transaccionRepository;
    public WebTransaccionController(TransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }
    @GetMapping
    public List<Transaccion> listar() {
        return transaccionRepository.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Transaccion> detalle(@PathVariable Long id) {
        return transaccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
