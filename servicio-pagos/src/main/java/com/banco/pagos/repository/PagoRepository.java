package com.banco.pagos.repository;

import com.banco.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(Long cuentaOrigen, Long cuentaDestino);
}
