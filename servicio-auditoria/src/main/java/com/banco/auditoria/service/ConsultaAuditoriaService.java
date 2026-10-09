package com.banco.auditoria.service;

import com.banco.auditoria.dto.EventoAuditoriaDTO;
import com.banco.auditoria.listener.AlertaAuditoriaListener;
import com.banco.auditoria.listener.RetiroAuditoriaListener;
import com.banco.auditoria.listener.TransaccionAuditoriaListener;
import com.banco.auditoria.model.EventoAuditoria;
import com.banco.auditoria.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultaAuditoriaService {

    private final EventoAuditoriaRepository repository;

    public ConsultaAuditoriaService(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    // Los 100 eventos mas recientes, con filtros opcionales por topico y tipo de evento.
    public List<EventoAuditoriaDTO> listar(String topico, String tipo) {
        boolean porTopico = topico != null && !topico.isBlank();
        boolean porTipo = tipo != null && !tipo.isBlank();
        List<EventoAuditoria> eventos;
        if (porTopico && porTipo) {
            eventos = repository.findTop100ByTopicoAndTipoEventoOrderByIdDesc(topico, tipo);
        } else if (porTopico) {
            eventos = repository.findTop100ByTopicoOrderByIdDesc(topico);
        } else if (porTipo) {
            eventos = repository.findTop100ByTipoEventoOrderByIdDesc(tipo);
        } else {
            eventos = repository.findTop100ByOrderByIdDesc();
        }
        return eventos.stream().map(EventoAuditoriaDTO::desde).toList();
    }

    // Conteo por topico; los topicos auditados aparecen siempre, con 0 si aun no tienen eventos.
    public Map<String, Long> resumen() {
        Map<String, Long> conteo = new LinkedHashMap<>();
        conteo.put(RetiroAuditoriaListener.TOPICO, 0L);
        conteo.put(TransaccionAuditoriaListener.TOPICO, 0L);
        conteo.put(AlertaAuditoriaListener.TOPICO, 0L);
        for (Object[] fila : repository.contarPorTopico()) {
            conteo.put((String) fila[0], (Long) fila[1]);
        }
        return conteo;
    }
}
