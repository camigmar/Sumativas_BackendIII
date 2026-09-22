package com.banco.core.controller;

import com.banco.batch.model.Transaccion;
import com.banco.core.service.TransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @GetMapping
    public List<Transaccion> listar() {
        return transaccionService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaccion> detalle(@PathVariable Long id) {
        return transaccionService.obtener(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
