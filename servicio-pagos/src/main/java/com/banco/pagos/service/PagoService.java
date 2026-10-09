package com.banco.pagos.service;

import com.banco.pagos.client.CuentasClient;
import com.banco.pagos.dto.DepositoRequestDTO;
import com.banco.pagos.dto.PagoResponseDTO;
import com.banco.pagos.dto.RetiroRequestDTO;
import com.banco.pagos.dto.TransferenciaRequestDTO;
import com.banco.pagos.event.PagoEventPublisher;
import com.banco.pagos.event.TipoAlerta;
import com.banco.pagos.exception.CuentasException;
import com.banco.pagos.exception.PagoFallidoException;
import com.banco.pagos.exception.PagoNoEncontradoException;
import com.banco.pagos.exception.TransferenciaInvalidaException;
import com.banco.pagos.model.EstadoPago;
import com.banco.pagos.model.Pago;
import com.banco.pagos.model.TipoPago;
import com.banco.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Sin @Transactional a proposito: cada save es una transaccion corta propia, asi no queda
// una transaccion de base de datos abierta mientras se espera a servicio-cuentas por HTTP.
@Service
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final CuentasClient cuentasClient;
    private final PagoEventPublisher eventPublisher;
    private final double montoElevado;

    public PagoService(PagoRepository pagoRepository, CuentasClient cuentasClient, PagoEventPublisher eventPublisher,
                       @Value("${alertas.monto-elevado:10000}") double montoElevado) {
        this.pagoRepository = pagoRepository;
        this.cuentasClient = cuentasClient;
        this.eventPublisher = eventPublisher;
        this.montoElevado = montoElevado;
    }

    public PagoResponseDTO retirar(RetiroRequestDTO request) {
        Pago pago = registrar(TipoPago.RETIRO, request.cuentaId(), null, request.monto());
        try {
            cuentasClient.debitar(request.cuentaId(), request.monto());
        } catch (CuentasException ex) {
            throw finalizarConError(pago, EstadoPago.FALLIDO, ex.getMessage(), ex.getStatus());
        }
        return completar(pago);
    }

    public PagoResponseDTO depositar(DepositoRequestDTO request) {
        Pago pago = registrar(TipoPago.DEPOSITO, request.cuentaId(), null, request.monto());
        try {
            cuentasClient.acreditar(request.cuentaId(), request.monto());
        } catch (CuentasException ex) {
            throw finalizarConError(pago, EstadoPago.FALLIDO, ex.getMessage(), ex.getStatus());
        }
        return completar(pago);
    }

    // Saga: debito en origen -> credito en destino. Si el credito falla, se compensa
    // devolviendo el monto a la cuenta origen.
    public PagoResponseDTO transferir(TransferenciaRequestDTO request) {
        if (request.cuentaOrigen().equals(request.cuentaDestino())) {
            throw new TransferenciaInvalidaException("La cuenta de origen y la de destino deben ser distintas");
        }

        Pago pago = registrar(TipoPago.TRANSFERENCIA, request.cuentaOrigen(), request.cuentaDestino(), request.monto());

        try {
            cuentasClient.debitar(pago.getCuentaOrigen(), pago.getMonto());
        } catch (CuentasException ex) {
            throw finalizarConError(pago, EstadoPago.FALLIDO,
                    "No se pudo debitar la cuenta origen: " + ex.getMessage(), ex.getStatus());
        }

        try {
            cuentasClient.acreditar(pago.getCuentaDestino(), pago.getMonto());
        } catch (CuentasException errorCredito) {
            throw compensar(pago, errorCredito);
        }

        return completar(pago);
    }

    public PagoResponseDTO obtener(Long id) {
        return pagoRepository.findById(id)
                .map(PagoResponseDTO::desde)
                .orElseThrow(() -> new PagoNoEncontradoException("Pago no encontrado"));
    }

    public List<PagoResponseDTO> listarPorCuenta(Long cuentaId) {
        return pagoRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(cuentaId, cuentaId).stream()
                .map(PagoResponseDTO::desde)
                .toList();
    }

    private PagoFallidoException compensar(Pago pago, CuentasException errorCredito) {
        String motivoCredito = "No se pudo acreditar la cuenta destino: " + errorCredito.getMessage();
        try {
            cuentasClient.acreditar(pago.getCuentaOrigen(), pago.getMonto());
        } catch (CuentasException errorCompensacion) {
            logger.error("Pago {} REQUIERE_REVISION: se debitaron {} de la cuenta {} pero no se acreditaron en la cuenta {} "
                            + "y la devolucion a la cuenta origen tambien fallo. Credito: {}. Compensacion: {}",
                    pago.getId(), pago.getMonto(), pago.getCuentaOrigen(), pago.getCuentaDestino(),
                    errorCredito.getMessage(), errorCompensacion.getMessage());
            return finalizarConError(pago, EstadoPago.REQUIERE_REVISION,
                    motivoCredito + ". La devolucion a la cuenta origen tambien fallo: " + errorCompensacion.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return finalizarConError(pago, EstadoPago.COMPENSADO,
                motivoCredito + ". Se devolvio el monto a la cuenta origen", errorCredito.getStatus());
    }

    private Pago registrar(TipoPago tipo, Long cuentaOrigen, Long cuentaDestino, Double monto) {
        Pago pago = new Pago();
        pago.setTipo(tipo);
        pago.setCuentaOrigen(cuentaOrigen);
        pago.setCuentaDestino(cuentaDestino);
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFecha(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    private PagoResponseDTO completar(Pago pago) {
        pago.setEstado(EstadoPago.COMPLETADO);
        Pago guardado = pagoRepository.save(pago);
        publicarSinFallar(() -> eventPublisher.publicarTransaccionCompletada(guardado));
        if (guardado.getMonto() >= montoElevado) {
            publicarSinFallar(() -> eventPublisher.publicarAlerta(TipoAlerta.MONTO_ELEVADO, guardado,
                    "Monto " + guardado.getMonto() + " mayor o igual al umbral " + montoElevado));
        }
        return PagoResponseDTO.desde(guardado);
    }

    // Guarda el estado final, publica la alerta que corresponda y devuelve la excepcion que el llamador lanza.
    private PagoFallidoException finalizarConError(Pago pago, EstadoPago estado, String motivo, HttpStatus status) {
        pago.setEstado(estado);
        pago.setMotivo(motivo);
        pagoRepository.save(pago);

        TipoAlerta alerta = switch (estado) {
            // Solo rechazos de negocio de cuentas (4xx); servicio-cuentas caido (503) no es una alerta de seguridad.
            case FALLIDO -> status.is4xxClientError() ? TipoAlerta.OPERACION_RECHAZADA : null;
            case COMPENSADO -> TipoAlerta.COMPENSACION_EJECUTADA;
            case REQUIERE_REVISION -> TipoAlerta.REQUIERE_REVISION;
            default -> null;
        };
        if (alerta != null) {
            publicarSinFallar(() -> eventPublisher.publicarAlerta(alerta, pago, motivo));
        }
        return new PagoFallidoException(pago.getId(), estado, status, motivo);
    }

    // El publisher ya no propaga errores de Kafka; esto cubre ademas cualquier falla inesperada
    // para que la publicacion nunca cambie el resultado de un pago ya guardado.
    private void publicarSinFallar(Runnable publicacion) {
        try {
            publicacion.run();
        } catch (RuntimeException ex) {
            logger.error("Fallo la publicacion de un evento de pago: {}", ex.getMessage());
        }
    }
}
