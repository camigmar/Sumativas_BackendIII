package com.banco.auditoria.controller;

import com.banco.auditoria.dto.EventoAuditoriaDTO;
import com.banco.auditoria.service.ConsultaAuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final ConsultaAuditoriaService consultaAuditoriaService;

    public AuditoriaController(ConsultaAuditoriaService consultaAuditoriaService) {
        this.consultaAuditoriaService = consultaAuditoriaService;
    }

    @GetMapping("/eventos")
    public List<EventoAuditoriaDTO> eventos(@RequestParam(required = false) String topico,
                                            @RequestParam(required = false) String tipo) {
        return consultaAuditoriaService.listar(topico, tipo);
    }

    @GetMapping("/resumen")
    public Map<String, Long> resumen() {
        return consultaAuditoriaService.resumen();
    }
}
