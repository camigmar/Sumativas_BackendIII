package com.banco.core.service;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.EstadoCuentaAnualRepository;
import com.banco.core.event.RetiroEventPublisher;
import com.banco.core.event.RetiroRealizadoEvent;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CuentaService {

    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final RetiroEventPublisher retiroEventPublisher;

    public CuentaService(CuentaInteresRepository cuentaInteresRepository,
                          EstadoCuentaAnualRepository estadoCuentaAnualRepository,
                          RetiroEventPublisher retiroEventPublisher) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.estadoCuentaAnualRepository = estadoCuentaAnualRepository;
        this.retiroEventPublisher = retiroEventPublisher;
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
        retiroEventPublisher.publicar(
                new RetiroRealizadoEvent(cuentaId, monto, nuevoSaldo, LocalDateTime.now().toString()));

        return nuevoSaldo;
    }

    // Debito y credito para servicio-pagos: no publican evento (lo hace pagos) y leen la cuenta
    // con bloqueo pesimista dentro de la transaccion para evitar carreras sobre el saldo.
    @Transactional
    public double debitar(Long cuentaId, Double monto) {
        validarMonto(monto);
        CuentaInteres cuenta = cuentaInteresRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));

        if (cuenta.getSaldo() == null || cuenta.getSaldo() < monto) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar el debito");
        }

        double nuevoSaldo = cuenta.getSaldo() - monto;
        cuenta.setSaldo(nuevoSaldo);
        cuentaInteresRepository.save(cuenta);
        return nuevoSaldo;
    }

    @Transactional
    public double acreditar(Long cuentaId, Double monto) {
        validarMonto(monto);
        CuentaInteres cuenta = cuentaInteresRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));

        double saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
        double nuevoSaldo = saldoActual + monto;
        cuenta.setSaldo(nuevoSaldo);
        cuentaInteresRepository.save(cuenta);
        return nuevoSaldo;
    }

    private void validarMonto(Double monto) {
        if (monto == null || monto <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor a 0");
        }
    }
}
