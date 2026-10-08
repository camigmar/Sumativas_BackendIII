package com.banco.batch.processor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import com.banco.batch.model.MovimientoAnual;

public class MovimientoProcessor implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    @Override
    public MovimientoAnual process(MovimientoAnual m) {
        if (m == null || m.getMonto() == null || m.getMonto() == 0) {
            return null;
        }
        if (m.getDescripcion() == null || m.getDescripcion().isBlank()) {
            return null;
        }
        if (m.getTransaccion() == null || m.getTransaccion().isBlank()) {
            return null;
        }
        m.setTransaccion(m.getTransaccion().trim().toLowerCase());
        return m;
    }
}
