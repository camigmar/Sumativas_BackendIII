package com.banco.bffweb.controller;

import com.banco.bffweb.dto.CuentaWebDTO;
import com.banco.bffweb.dto.MovimientoDTO;
import com.banco.bffweb.service.WebCuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web/cuentas")
public class WebCuentaController {

    private final WebCuentaService webCuentaService;

    public WebCuentaController(WebCuentaService webCuentaService) {
        this.webCuentaService = webCuentaService;
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaWebDTO> detalle(@PathVariable Long cuentaId) {
        return webCuentaService.obtenerDetalle(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoDTO> movimientos(@PathVariable Long cuentaId) {
        return webCuentaService.obtenerMovimientos(cuentaId);
    }
}
