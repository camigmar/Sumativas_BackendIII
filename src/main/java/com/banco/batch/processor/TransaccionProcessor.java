package com.banco.batch.processor;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.banco.batch.model.Transaccion;

public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion> {

    public Transaccion process(Transaccion t) {
        if (t == null || t.getMonto() == null || t.getMonto() == 0) {
            return null;
        }
        if(t.getTipo() != null){
            t.setTipo(t.getTipo().trim().toLowerCase());
        }
        return t;
    }
}