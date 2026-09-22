package com.banco.core.service;

import com.banco.batch.model.MovimientoAnual;
import com.banco.batch.repository.MovimientoAnualRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoAnualRepository movimientoAnualRepository;

    public MovimientoService(MovimientoAnualRepository movimientoAnualRepository) {
        this.movimientoAnualRepository = movimientoAnualRepository;
    }

    public List<MovimientoAnual> obtenerMovimientos(Long cuentaId) {
        return movimientoAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId);
    }

    public List<MovimientoAnual> obtenerUltimosMovimientos(Long cuentaId, int limite) {
        return movimientoAnualRepository.findByCuentaIdOrderByFechaDesc(cuentaId, PageRequest.of(0, limite));
    }
}
