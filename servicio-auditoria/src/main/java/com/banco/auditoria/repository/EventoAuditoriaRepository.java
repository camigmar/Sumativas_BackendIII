package com.banco.auditoria.repository;

import com.banco.auditoria.model.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    boolean existsByTopicoAndParticionAndOffset(String topico, Integer particion, Long offset);

    List<EventoAuditoria> findTop100ByOrderByIdDesc();
    List<EventoAuditoria> findTop100ByTopicoOrderByIdDesc(String topico);
    List<EventoAuditoria> findTop100ByTipoEventoOrderByIdDesc(String tipoEvento);
    List<EventoAuditoria> findTop100ByTopicoAndTipoEventoOrderByIdDesc(String topico, String tipoEvento);

    @Query("select e.topico, count(e) from EventoAuditoria e group by e.topico")
    List<Object[]> contarPorTopico();
}
