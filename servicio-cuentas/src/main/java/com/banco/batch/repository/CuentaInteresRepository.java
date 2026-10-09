package com.banco.batch.repository;
import com.banco.batch.model.CuentaInteres;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaInteresRepository extends JpaRepository<CuentaInteres, Long> {

    // SELECT ... FOR UPDATE: bloquea la fila hasta el fin de la transaccion para que dos
    // debitos/creditos concurrentes sobre la misma cuenta no pisen el saldo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaInteres c where c.cuentaId = :cuentaId")
    Optional<CuentaInteres> findByIdForUpdate(@Param("cuentaId") Long cuentaId);

    // Cuenta con el mayor id, bloqueada (SELECT ... ORDER BY cuenta_id DESC LIMIT 1 FOR UPDATE):
    // dos aperturas simultaneas se serializan aqui y no pueden calcular el mismo id siguiente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CuentaInteres> findFirstByOrderByCuentaIdDesc();

    List<CuentaInteres> findByClienteIdOrderByCuentaIdAsc(Long clienteId);
}
