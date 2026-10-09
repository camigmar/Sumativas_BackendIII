package com.banco.core.service;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuenta;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.repository.CuentaInteresRepository;
import com.banco.batch.repository.EstadoCuentaAnualRepository;
import com.banco.core.client.ClientesClient;
import com.banco.core.dto.ActualizarCuentaRequestDTO;
import com.banco.core.dto.AperturaCuentaRequestDTO;
import com.banco.core.dto.ClienteDTO;
import com.banco.core.event.RetiroEventPublisher;
import com.banco.core.event.RetiroRealizadoEvent;
import com.banco.core.exception.CierreNoPermitidoException;
import com.banco.core.exception.ClienteInactivoException;
import com.banco.core.exception.ClientesNoDisponibleException;
import com.banco.core.exception.CuentaCerradaException;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CuentaService {

    private final CuentaInteresRepository cuentaInteresRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final RetiroEventPublisher retiroEventPublisher;
    private final ClientesClient clientesClient;
    private final TransactionTemplate transactionTemplate;

    public CuentaService(CuentaInteresRepository cuentaInteresRepository,
                          EstadoCuentaAnualRepository estadoCuentaAnualRepository,
                          RetiroEventPublisher retiroEventPublisher,
                          ClientesClient clientesClient,
                          TransactionTemplate transactionTemplate) {
        this.cuentaInteresRepository = cuentaInteresRepository;
        this.estadoCuentaAnualRepository = estadoCuentaAnualRepository;
        this.retiroEventPublisher = retiroEventPublisher;
        this.clientesClient = clientesClient;
        this.transactionTemplate = transactionTemplate;
    }

    // La consulta a servicio-clientes va fuera de la transaccion; solo el calculo del id y el
    // insert van dentro, con la cuenta de mayor id bloqueada para que dos aperturas no choquen.
    public CuentaInteres abrirCuenta(AperturaCuentaRequestDTO request) {
        ClienteDTO cliente = clientesClient.obtenerCliente(request.clienteId());
        if (cliente == null) {
            throw new ClientesNoDisponibleException("servicio-clientes respondio sin datos del cliente", null);
        }
        if ("INACTIVO".equals(cliente.estado())) {
            throw new ClienteInactivoException("El cliente " + request.clienteId() + " esta inactivo");
        }

        return transactionTemplate.execute(status -> {
            long siguienteId = cuentaInteresRepository.findFirstByOrderByCuentaIdDesc()
                    .map(ultima -> ultima.getCuentaId() + 1)
                    .orElse(1L);

            CuentaInteres cuenta = new CuentaInteres();
            cuenta.setCuentaId(siguienteId);
            cuenta.setClienteId(request.clienteId());
            cuenta.setNombre(request.nombre());
            cuenta.setEdad(request.edad());
            cuenta.setTipo(request.tipo());
            cuenta.setSaldo(request.saldoInicial());
            cuenta.setEstado(EstadoCuenta.ACTIVA);
            return cuentaInteresRepository.save(cuenta);
        });
    }

    @Transactional
    public CuentaInteres actualizarCuenta(Long cuentaId, ActualizarCuentaRequestDTO request) {
        CuentaInteres cuenta = buscarParaActualizar(cuentaId);
        verificarAbierta(cuenta);

        cuenta.setNombre(request.nombre());
        cuenta.setEdad(request.edad());
        cuenta.setTipo(request.tipo());
        return cuentaInteresRepository.save(cuenta);
    }

    @Transactional
    public CuentaInteres cerrarCuenta(Long cuentaId) {
        CuentaInteres cuenta = buscarParaActualizar(cuentaId);
        if (cuenta.estaCerrada()) {
            throw new CuentaCerradaException("La cuenta " + cuentaId + " ya esta cerrada");
        }
        double saldo = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
        if (saldo != 0.0) {
            throw new CierreNoPermitidoException("No se puede cerrar la cuenta " + cuentaId
                    + ": el saldo debe ser 0 y es " + saldo);
        }

        cuenta.setEstado(EstadoCuenta.CERRADA);
        return cuentaInteresRepository.save(cuenta);
    }

    public List<CuentaInteres> listarPorCliente(Long clienteId) {
        return cuentaInteresRepository.findByClienteIdOrderByCuentaIdAsc(clienteId);
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
        verificarAbierta(cuenta);

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
        CuentaInteres cuenta = buscarParaActualizar(cuentaId);
        verificarAbierta(cuenta);

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
        CuentaInteres cuenta = buscarParaActualizar(cuentaId);
        verificarAbierta(cuenta);

        double saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
        double nuevoSaldo = saldoActual + monto;
        cuenta.setSaldo(nuevoSaldo);
        cuentaInteresRepository.save(cuenta);
        return nuevoSaldo;
    }

    // Lee la cuenta con bloqueo pesimista; debe llamarse dentro de una transaccion.
    private CuentaInteres buscarParaActualizar(Long cuentaId) {
        return cuentaInteresRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));
    }

    private void verificarAbierta(CuentaInteres cuenta) {
        if (cuenta.estaCerrada()) {
            throw new CuentaCerradaException("La cuenta " + cuenta.getCuentaId() + " esta cerrada");
        }
    }

    private void validarMonto(Double monto) {
        if (monto == null || monto <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor a 0");
        }
    }
}
