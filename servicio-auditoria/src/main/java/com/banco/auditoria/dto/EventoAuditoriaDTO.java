package com.banco.auditoria.dto;

import com.banco.auditoria.model.EventoAuditoria;

import java.time.LocalDateTime;

public record EventoAuditoriaDTO(
        Long id,
        String topico,
        String tipoEvento,
        String clave,
        String payload,
        Integer particion,
        Long offset,
        LocalDateTime fechaRegistro) {

    public static EventoAuditoriaDTO desde(EventoAuditoria evento) {
        return new EventoAuditoriaDTO(
                evento.getId(),
                evento.getTopico(),
                evento.getTipoEvento(),
                evento.getClave(),
                evento.getPayload(),
                evento.getParticion(),
                evento.getOffset(),
                evento.getFechaRegistro());
    }
}
