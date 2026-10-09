package com.banco.core.repository;

import com.banco.core.model.MovimientoIdempotente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoIdempotenteRepository extends JpaRepository<MovimientoIdempotente, String> {
}
