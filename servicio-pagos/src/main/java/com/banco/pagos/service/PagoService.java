package com.banco.pagos.service;

import com.banco.pagos.client.CuentasClient;
import com.banco.pagos.dto.DepositoRequestDTO;
import com.banco.pagos.dto.PagoResponseDTO;
import com.banco.pagos.dto.RetiroRequestDTO;
import com.banco.pagos.dto.TransferenciaRequestDTO;
import com.banco.pagos.event.PagoEventPublisher;
import com.banco.pagos.event.TipoAlerta;
import com.banco.pagos.exception.ClaveIdempotenciaReutilizadaException;
import com.banco.pagos.exception.CuentasException;
import com.banco.pagos.exception.IdempotencyKeyInvalidaException;
import com.banco.pagos.exception.PagoFallidoException;
import com.banco.pagos.exception.PagoNoEncontradoException;
import com.banco.pagos.exception.SolicitudEnProcesoException;
import com.banco.pagos.exception.TransferenciaInvalidaException;
import com.banco.pagos.model.EstadoPago;
import com.banco.pagos.model.Pago;
import com.banco.pagos.model.TipoPago;
import com.banco.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// Sin @Transactional a proposito: cada save es una transaccion corta propia, asi no queda
// una transaccion de base de datos abierta mientras se espera a servicio-cuentas por HTTP.
@Service
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    static final String OPERACION_DEBITO = "debito";
    static final String OPERACION_CREDITO = "credito";
    static final String OPERACION_COMPENSACION = "compensacion";
    static final int LARGO_MAXIMO_CLAVE = 100;

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

    // claveIdempotencia: header Idempotency-Key opcional. Sin clave, cada solicitud crea un pago nuevo.
    public PagoResponseDTO retirar(RetiroRequestDTO request, String claveIdempotencia) {
        return ejecutarIdempotente(claveIdempotencia, TipoPago.RETIRO, request.cuentaId(), null, request.monto(), pago -> {
            try {
                cuentasClient.debitar(pago.getCuentaOrigen(), pago.getMonto(), claveMovimiento(pago, OPERACION_DEBITO));
            } catch (CuentasException ex) {
                throw finalizarConError(pago, EstadoPago.FALLIDO, ex.getMessage(), ex.getStatus());
            }
            return completar(pago);
        });
    }

    public PagoResponseDTO depositar(DepositoRequestDTO request, String claveIdempotencia) {
        return ejecutarIdempotente(claveIdempotencia, TipoPago.DEPOSITO, request.cuentaId(), null, request.monto(), pago -> {
            try {
                cuentasClient.acreditar(pago.getCuentaOrigen(), pago.getMonto(), claveMovimiento(pago, OPERACION_CREDITO));
            } catch (CuentasException ex) {
                throw finalizarConError(pago, EstadoPago.FALLIDO, ex.getMessage(), ex.getStatus());
            }
            return completar(pago);
        });
    }

    // Saga: debito en origen -> credito en destino. Si el credito falla, se compensa
    // devolviendo el monto a la cuenta origen.
    public PagoResponseDTO transferir(TransferenciaRequestDTO request, String claveIdempotencia) {
        if (request.cuentaOrigen().equals(request.cuentaDestino())) {
            throw new TransferenciaInvalidaException("La cuenta de origen y la de destino deben ser distintas");
        }

        return ejecutarIdempotente(claveIdempotencia, TipoPago.TRANSFERENCIA, request.cuentaOrigen(),
                request.cuentaDestino(), request.monto(), pago -> {
            try {
                cuentasClient.debitar(pago.getCuentaOrigen(), pago.getMonto(), claveMovimiento(pago, OPERACION_DEBITO));
            } catch (CuentasException ex) {
                throw finalizarConError(pago, EstadoPago.FALLIDO,
                        "No se pudo debitar la cuenta origen: " + ex.getMessage(), ex.getStatus());
            }

            try {
                cuentasClient.acreditar(pago.getCuentaDestino(), pago.getMonto(), claveMovimiento(pago, OPERACION_CREDITO));
            } catch (CuentasException errorCredito) {
                throw compensar(pago, errorCredito);
            }

            return completar(pago);
        });
    }

    // Idempotencia hacia el cliente: la misma Idempotency-Key con la misma solicitud devuelve el resultado
    // original (201 o el mismo error) sin volver a llamar a cuentas; con otra solicitud responde 422.
    // Dos solicitudes simultaneas con la misma clave chocan en la restriccion unica: solo una ejecuta.
    private PagoResponseDTO ejecutarIdempotente(String claveIdempotencia, TipoPago tipo, Long cuentaOrigen,
                                                Long cuentaDestino, Double monto, Function<Pago, PagoResponseDTO> saga) {
        String clave = normalizarClave(claveIdempotencia);
        String huella = tipo + "|" + cuentaOrigen + "|" + cuentaDestino + "|" + monto;
        if (clave != null) {
            Optional<Pago> previo = pagoRepository.findByIdempotencyKey(clave);
            if (previo.isPresent()) {
                return repetir(previo.get(), huella);
            }
        }

        Pago pago;
        try {
            pago = registrar(tipo, cuentaOrigen, cuentaDestino, monto, clave, huella);
        } catch (DataIntegrityViolationException ex) {
            if (clave == null) {
                throw ex;
            }
            // Otra solicitud con la misma clave se registro entre la consulta y el insert.
            return repetir(pagoRepository.findByIdempotencyKey(clave).orElseThrow(() -> ex), huella);
        }
        return saga.apply(pago);
    }

    private PagoResponseDTO repetir(Pago original, String huella) {
        if (!huella.equals(original.getHuellaSolicitud())) {
            throw new ClaveIdempotenciaReutilizadaException("La Idempotency-Key " + original.getIdempotencyKey()
                    + " ya se uso con otra solicitud (pago " + original.getId() + ")");
        }
        return switch (original.getEstado()) {
            case PENDIENTE -> throw new SolicitudEnProcesoException("La solicitud con Idempotency-Key "
                    + original.getIdempotencyKey() + " aun esta en proceso (pago " + original.getId() + ")");
            case COMPLETADO -> PagoResponseDTO.desde(original);
            default -> throw new PagoFallidoException(original.getId(), original.getEstado(),
                    HttpStatus.valueOf(original.getStatusRespuesta() != null ? original.getStatusRespuesta() : 500),
                    original.getMotivo());
        };
    }

    private String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        if (clave.length() > LARGO_MAXIMO_CLAVE) {
            throw new IdempotencyKeyInvalidaException("La Idempotency-Key no puede superar "
                    + LARGO_MAXIMO_CLAVE + " caracteres");
        }
        return clave.trim();
    }

    // Clave de cada movimiento hacia servicio-cuentas: los reintentos de Resilience4j (y cualquier
    // repeticion) usan la misma clave, asi cuentas nunca aplica dos veces un debito o un credito.
    static String claveMovimiento(Pago pago, String operacion) {
        return "pago-" + pago.getId() + "-" + operacion;
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
            cuentasClient.acreditar(pago.getCuentaOrigen(), pago.getMonto(), claveMovimiento(pago, OPERACION_COMPENSACION));
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

    // Con id IDENTITY el insert es inmediato: una clave repetida falla aqui con DataIntegrityViolationException.
    private Pago registrar(TipoPago tipo, Long cuentaOrigen, Long cuentaDestino, Double monto,
                           String claveIdempotencia, String huella) {
        Pago pago = new Pago();
        pago.setTipo(tipo);
        pago.setCuentaOrigen(cuentaOrigen);
        pago.setCuentaDestino(cuentaDestino);
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFecha(LocalDateTime.now());
        pago.setIdempotencyKey(claveIdempotencia);
        pago.setHuellaSolicitud(huella);
        return pagoRepository.save(pago);
    }

    private PagoResponseDTO completar(Pago pago) {
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setStatusRespuesta(HttpStatus.CREATED.value());
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
        pago.setStatusRespuesta(status.value());
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
