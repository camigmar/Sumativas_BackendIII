package com.banco.batch.repository;
import com.banco.batch.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
}
