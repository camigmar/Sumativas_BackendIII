package com.banco.core.service;

import com.banco.batch.model.Transaccion;
import com.banco.batch.repository.TransaccionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;

    public TransaccionService(TransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }

    public List<Transaccion> listar() {
        return transaccionRepository.findAll();
    }

    public Optional<Transaccion> obtener(Long id) {
        return transaccionRepository.findById(id);
    }
}
