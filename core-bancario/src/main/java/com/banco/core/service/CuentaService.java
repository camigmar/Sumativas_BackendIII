package com.banco.core.service;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.EstadoCuentaAnualRepository;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CuentaService {

    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;

    public CuentaService(CuentaInteresRepository cuentaInteresRepository,
                          EstadoCuentaAnualRepository estadoCuentaAnualRepository) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.estadoCuentaAnualRepository = estadoCuentaAnualRepository;
    }

    public Optional<CuentaInteres> obtenerCuenta(Long cuentaId) {
        return cuentaInteresRepository.findById(cuentaId);
    }

    public boolean existeCuenta(Long cuentaId) {
        return cuentaInteresRepository.existsById(cuentaId);
    }

    public Optional<EstadoCuentaAnual> obtenerEstadoAnual(Long cuentaId) {
        return estadoCuentaAnualRepository.findById(cuentaId);
    }

    public double retirar(Long cuentaId, Double monto) {
        if (monto == null || monto <= 0) {
            throw new MontoInvalidoException("El monto a retirar debe ser mayor a 0");
        }

        CuentaInteres cuenta = cuentaInteresRepository.findById(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));

        if (cuenta.getSaldo() == null || cuenta.getSaldo() < monto) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar el retiro");
        }

        double nuevoSaldo = cuenta.getSaldo() - monto;
        cuenta.setSaldo(nuevoSaldo);
        cuentaInteresRepository.save(cuenta);

        return nuevoSaldo;
    }
}
