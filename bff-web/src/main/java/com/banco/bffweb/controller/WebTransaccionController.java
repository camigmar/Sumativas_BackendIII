package com.banco.bffweb.controller;

import com.banco.bffweb.dto.TransaccionDTO;
import com.banco.bffweb.service.WebTransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web/transacciones")
public class WebTransaccionController {

    private final WebTransaccionService webTransaccionService;

    public WebTransaccionController(WebTransaccionService webTransaccionService) {
        this.webTransaccionService = webTransaccionService;
    }

    @GetMapping
    public List<TransaccionDTO> listar() {
        return webTransaccionService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionDTO> detalle(@PathVariable Long id) {
        return webTransaccionService.obtener(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
