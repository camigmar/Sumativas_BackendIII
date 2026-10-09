package com.banco.pagos.service;

import com.banco.pagos.client.CuentasClient;
import com.banco.pagos.dto.DepositoRequestDTO;
import com.banco.pagos.dto.PagoResponseDTO;
import com.banco.pagos.dto.RetiroRequestDTO;
import com.banco.pagos.dto.TransferenciaRequestDTO;
import com.banco.pagos.event.PagoEventPublisher;
import com.banco.pagos.event.TipoAlerta;
import com.banco.pagos.exception.ClaveIdempotenciaReutilizadaException;
import com.banco.pagos.exception.CuentaNoEncontradaException;
import com.banco.pagos.exception.CuentasNoDisponibleException;
import com.banco.pagos.exception.IdempotencyKeyInvalidaException;
import com.banco.pagos.exception.OperacionRechazadaException;
import com.banco.pagos.exception.PagoFallidoException;
import com.banco.pagos.exception.SolicitudEnProcesoException;
import com.banco.pagos.exception.TransferenciaInvalidaException;
import com.banco.pagos.model.EstadoPago;
import com.banco.pagos.model.Pago;
import com.banco.pagos.model.TipoPago;
import com.banco.pagos.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final double UMBRAL_MONTO_ELEVADO = 10000.0;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CuentasClient cuentasClient;

    @Mock
    private PagoEventPublisher eventPublisher;

    private PagoService pagoService;

    // Estado del pago en cada save, en orden (el mock recibe siempre la misma instancia).
    private final List<EstadoPago> estadosGuardados = new ArrayList<>();
    private Pago pagoGuardado;

    @BeforeEach
    void setUp() {
        // Construccion manual: el umbral es un double inyectado con @Value.
        pagoService = new PagoService(pagoRepository, cuentasClient, eventPublisher, UMBRAL_MONTO_ELEVADO);
    }

    private void simularGuardado() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            pago.setId(1L);
            estadosGuardados.add(pago.getEstado());
            pagoGuardado = pago;
            return pago;
        });
    }

    @Test
    void retirar_casoExitosoQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.retirar(new RetiroRequestDTO(101L, 200.0), null);

        verify(cuentasClient).debitar(101L, 200.0, "pago-1-debito");
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.tipo()).isEqualTo(TipoPago.RETIRO);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(respuesta.cuentaDestino()).isNull();
    }

    @Test
    void retirar_saldoInsuficienteQuedaFallidoConMotivoY409() {
        simularGuardado();
        when(cuentasClient.debitar(101L, 900.0, "pago-1-debito"))
                .thenThrow(new OperacionRechazadaException("Saldo insuficiente para realizar el debito"));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(101L, 900.0), null));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.FALLIDO);
        assertThat(pagoGuardado.getMotivo()).contains("Saldo insuficiente");
    }

    @Test
    void depositar_casoExitosoQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.depositar(new DepositoRequestDTO(102L, 300.0), null);

        verify(cuentasClient).acreditar(102L, 300.0, "pago-1-credito");
        verify(cuentasClient, never()).debitar(any(), any(), any());
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.tipo()).isEqualTo(TipoPago.DEPOSITO);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.COMPLETADO);
    }

    @Test
    void transferir_casoExitosoDebitaOrigenAcreditaDestinoYQuedaCompletado() {
        simularGuardado();

        PagoResponseDTO respuesta = pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, 150.0), null);

        var orden = inOrder(cuentasClient);
        orden.verify(cuentasClient).debitar(101L, 150.0, "pago-1-debito");
        orden.verify(cuentasClient).acreditar(102L, 150.0, "pago-1-credito");
        orden.verifyNoMoreInteractions();
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
        assertThat(respuesta.cuentaOrigen()).isEqualTo(101L);
        assertThat(respuesta.cuentaDestino()).isEqualTo(102L);
    }

    @Test
    void transferir_creditoFallidoSeCompensaYQuedaCompensado() {
        simularGuardado();
        when(cuentasClient.acreditar(999L, 150.0, "pago-1-credito"))
                .thenThrow(new CuentaNoEncontradaException("La cuenta 999 no existe"));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 999L, 150.0), null));

        var orden = inOrder(cuentasClient);
        orden.verify(cuentasClient).debitar(101L, 150.0, "pago-1-debito");
        orden.verify(cuentasClient).acreditar(999L, 150.0, "pago-1-credito");
        orden.verify(cuentasClient).acreditar(101L, 150.0, "pago-1-compensacion");
        assertThat(ex.getEstado()).isEqualTo(EstadoPago.COMPENSADO);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPENSADO);
        assertThat(pagoGuardado.getMotivo()).contains("La cuenta 999 no existe");
    }

    @Test
    void transferir_compensacionFallidaQuedaRequiereRevision() {
        simularGuardado();
        when(cuentasClient.acreditar(102L, 150.0, "pago-1-credito"))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));
        when(cuentasClient.acreditar(101L, 150.0, "pago-1-compensacion"))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, 150.0), null));

        assertThat(ex.getEstado()).isEqualTo(EstadoPago.REQUIERE_REVISION);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.REQUIERE_REVISION);
        assertThat(pagoGuardado.getMotivo()).contains("La devolucion a la cuenta origen tambien fallo");
    }

    @Test
    void transferir_mismaCuentaLanzaExcepcionSinRegistrarNiLlamarACuentas() {
        assertThrows(TransferenciaInvalidaException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 101L, 150.0), null));
        verifyNoInteractions(pagoRepository);
        verifyNoInteractions(cuentasClient);
        verifyNoInteractions(eventPublisher);
    }

    // ------------------------------------------------------------------ Eventos

    @Test
    void pagoCompletado_publicaTransaccionCompletadaSinAlertas() {
        simularGuardado();

        pagoService.depositar(new DepositoRequestDTO(102L, 300.0), null);

        verify(eventPublisher).publicarTransaccionCompletada(pagoGuardado);
        verify(eventPublisher, never()).publicarAlerta(any(), any(), anyString());
    }

    @Test
    void pagoFallidoPorRechazoDeCuentas_publicaOperacionRechazadaYNoCompletado() {
        simularGuardado();
        when(cuentasClient.debitar(109L, 10.0, "pago-1-debito"))
                .thenThrow(new OperacionRechazadaException("La cuenta 109 esta cerrada"));

        assertThrows(PagoFallidoException.class, () -> pagoService.retirar(new RetiroRequestDTO(109L, 10.0), null));

        verify(eventPublisher).publicarAlerta(TipoAlerta.OPERACION_RECHAZADA, pagoGuardado, "La cuenta 109 esta cerrada");
        verify(eventPublisher, never()).publicarTransaccionCompletada(any());
    }

    @Test
    void pagoFallidoPorCuentasCaido_noPublicaAlerta() {
        simularGuardado();
        when(cuentasClient.debitar(101L, 10.0, "pago-1-debito"))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(101L, 10.0), null));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transferenciaCompensada_publicaCompensacionEjecutada() {
        simularGuardado();
        when(cuentasClient.acreditar(999L, 150.0, "pago-1-credito"))
                .thenThrow(new CuentaNoEncontradaException("La cuenta 999 no existe"));

        assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 999L, 150.0), null));

        verify(eventPublisher).publicarAlerta(eq(TipoAlerta.COMPENSACION_EJECUTADA), eq(pagoGuardado), anyString());
        verify(eventPublisher, never()).publicarTransaccionCompletada(any());
    }

    @Test
    void transferenciaSinCompensar_publicaRequiereRevision() {
        simularGuardado();
        when(cuentasClient.acreditar(102L, 150.0, "pago-1-credito"))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));
        when(cuentasClient.acreditar(101L, 150.0, "pago-1-compensacion"))
                .thenThrow(new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null));

        assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, 150.0), null));

        verify(eventPublisher).publicarAlerta(eq(TipoAlerta.REQUIERE_REVISION), eq(pagoGuardado), anyString());
    }

    @Test
    void pagoSobreElUmbral_publicaCompletadoYMontoElevado() {
        simularGuardado();

        pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, UMBRAL_MONTO_ELEVADO), null);

        verify(eventPublisher).publicarTransaccionCompletada(pagoGuardado);
        verify(eventPublisher).publicarAlerta(eq(TipoAlerta.MONTO_ELEVADO), eq(pagoGuardado), anyString());
    }

    @Test
    void pagoBajoElUmbral_noPublicaMontoElevado() {
        simularGuardado();

        pagoService.transferir(new TransferenciaRequestDTO(101L, 102L, UMBRAL_MONTO_ELEVADO - 1), null);

        verify(eventPublisher, never()).publicarAlerta(eq(TipoAlerta.MONTO_ELEVADO), any(), anyString());
    }

    @Test
    void fallaDelPublisher_noCambiaElResultadoDelPago() {
        simularGuardado();
        doThrow(new RuntimeException("Kafka caido")).when(eventPublisher).publicarTransaccionCompletada(any());

        PagoResponseDTO respuesta = pagoService.depositar(new DepositoRequestDTO(102L, 300.0), null);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(estadosGuardados).containsExactly(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO);
    }

    @Test
    void fallaDelPublisherEnUnaAlerta_mantieneElErrorOriginalDelPago() {
        simularGuardado();
        when(cuentasClient.debitar(101L, 900.0, "pago-1-debito"))
                .thenThrow(new OperacionRechazadaException("Saldo insuficiente para realizar el debito"));
        doThrow(new RuntimeException("Kafka caido")).when(eventPublisher).publicarAlerta(any(), any(), anyString());

        PagoFallidoException ex = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(101L, 900.0), null));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getEstado()).isEqualTo(EstadoPago.FALLIDO);
    }

    // ------------------------------------------------------------------ Idempotencia

    // Repositorio en memoria con la restriccion unica de idempotency_key (thread-safe).
    private void simularRepositorioConClaveUnica() {
        Map<String, Pago> porClave = new ConcurrentHashMap<>();
        AtomicLong secuencia = new AtomicLong();
        when(pagoRepository.findByIdempotencyKey(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(porClave.get((String) invocacion.getArgument(0))));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            if (pago.getId() == null) {
                if (pago.getIdempotencyKey() != null && porClave.putIfAbsent(pago.getIdempotencyKey(), pago) != null) {
                    throw new DataIntegrityViolationException("Duplicate entry for key idempotency_key");
                }
                pago.setId(secuencia.incrementAndGet());
            }
            return pago;
        });
    }

    @Test
    void mismaClaveMismoCuerpo_devuelveElPagoOriginalSinVolverALlamarACuentas() {
        simularRepositorioConClaveUnica();

        PagoResponseDTO primera = pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-1");
        PagoResponseDTO segunda = pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-1");

        assertThat(segunda).isEqualTo(primera);
        assertThat(segunda.estado()).isEqualTo(EstadoPago.COMPLETADO);
        verify(cuentasClient, times(1)).acreditar(102L, 300.0, "pago-1-credito");
        verify(eventPublisher, times(1)).publicarTransaccionCompletada(any());
    }

    @Test
    void mismaClaveConOtroCuerpo_lanza422SinEjecutar() {
        simularRepositorioConClaveUnica();
        pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-1");

        assertThrows(ClaveIdempotenciaReutilizadaException.class,
                () -> pagoService.depositar(new DepositoRequestDTO(102L, 999.0), "clave-1"));
        assertThrows(ClaveIdempotenciaReutilizadaException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(102L, 300.0), "clave-1"));
        verify(cuentasClient, times(1)).acreditar(any(), any(), any());
        verify(cuentasClient, never()).debitar(any(), any(), any());
    }

    @Test
    void mismaClaveDeUnPagoFallido_repiteElMismoErrorSinEjecutar() {
        simularRepositorioConClaveUnica();
        when(cuentasClient.debitar(105L, 999999.0, "pago-1-debito"))
                .thenThrow(new OperacionRechazadaException("Saldo insuficiente para realizar el debito"));
        PagoFallidoException original = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(105L, 999999.0), "clave-2"));

        PagoFallidoException repetido = assertThrows(PagoFallidoException.class,
                () -> pagoService.retirar(new RetiroRequestDTO(105L, 999999.0), "clave-2"));

        assertThat(repetido.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getMessage()).isEqualTo(original.getMessage());
        verify(cuentasClient, times(1)).debitar(any(), any(), any());
    }

    @Test
    void mismaClaveMientrasElPagoSigueEnCurso_lanzaSolicitudEnProceso() {
        Pago enCurso = new Pago();
        enCurso.setId(4L);
        enCurso.setIdempotencyKey("clave-3");
        enCurso.setHuellaSolicitud("DEPOSITO|102|null|300.0");
        enCurso.setEstado(EstadoPago.PENDIENTE);
        when(pagoRepository.findByIdempotencyKey("clave-3")).thenReturn(Optional.of(enCurso));

        assertThrows(SolicitudEnProcesoException.class,
                () -> pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-3"));
        verifyNoInteractions(cuentasClient);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void choqueEnLaRestriccionUnica_seResuelveConElPagoQueGano() {
        Pago ganador = new Pago();
        ganador.setId(9L);
        ganador.setIdempotencyKey("clave-4");
        ganador.setHuellaSolicitud("DEPOSITO|102|null|300.0");
        ganador.setEstado(EstadoPago.PENDIENTE);
        when(pagoRepository.findByIdempotencyKey("clave-4")).thenReturn(Optional.empty(), Optional.of(ganador));
        when(pagoRepository.save(any(Pago.class))).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(SolicitudEnProcesoException.class,
                () -> pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-4"));
        verifyNoInteractions(cuentasClient);
    }

    @Test
    void dosSolicitudesSimultaneasConLaMismaClave_soloUnaMueveSaldo() throws Exception {
        simularRepositorioConClaveUnica();
        when(cuentasClient.acreditar(any(), any(), any())).thenAnswer(invocacion -> {
            Thread.sleep(300);
            return null;
        });
        CountDownLatch partida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        Callable<Object> solicitud = () -> {
            partida.await();
            try {
                return pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "clave-concurrente");
            } catch (RuntimeException ex) {
                return ex;
            }
        };
        Future<Object> a = hilos.submit(solicitud);
        Future<Object> b = hilos.submit(solicitud);

        partida.countDown();
        List<Object> resultados = List.of(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
        hilos.shutdown();

        verify(cuentasClient, times(1)).acreditar(any(), any(), any());
        assertThat(resultados).filteredOn(r -> r instanceof PagoResponseDTO).hasSize(1);
        assertThat(resultados).filteredOn(r -> r instanceof SolicitudEnProcesoException).hasSize(1);
    }

    @Test
    void claveDemasiadoLarga_lanzaIdempotencyKeyInvalida() {
        assertThrows(IdempotencyKeyInvalidaException.class,
                () -> pagoService.depositar(new DepositoRequestDTO(102L, 300.0), "x".repeat(101)));
        verifyNoInteractions(pagoRepository, cuentasClient);
    }

    @Test
    void transferenciaCompensada_enviaUnaClaveDistintaPorMovimiento() {
        simularGuardado();
        when(cuentasClient.acreditar(999L, 150.0, "pago-1-credito"))
                .thenThrow(new CuentaNoEncontradaException("La cuenta 999 no existe"));

        assertThrows(PagoFallidoException.class,
                () -> pagoService.transferir(new TransferenciaRequestDTO(101L, 999L, 150.0), "clave-5"));

        verify(cuentasClient).debitar(101L, 150.0, "pago-1-debito");
        verify(cuentasClient).acreditar(999L, 150.0, "pago-1-credito");
        verify(cuentasClient).acreditar(101L, 150.0, "pago-1-compensacion");
        assertThat(pagoGuardado.getIdempotencyKey()).isEqualTo("clave-5");
        assertThat(pagoGuardado.getStatusRespuesta()).isEqualTo(404);
    }
}
