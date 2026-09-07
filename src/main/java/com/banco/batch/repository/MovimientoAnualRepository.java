package com.banco.batch.repository;
import com.banco.batch.model.MovimientoAnual;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MovimientoAnualRepository extends JpaRepository<MovimientoAnual, Long> {
    List<MovimientoAnual> findByCuentaIdOrderByFechaDesc(Long cuentaId);
}
