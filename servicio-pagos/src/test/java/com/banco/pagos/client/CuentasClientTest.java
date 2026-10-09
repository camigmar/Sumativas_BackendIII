package com.banco.pagos.client;

import com.banco.pagos.exception.CuentasNoDisponibleException;
import com.banco.pagos.exception.CuentasRespuestaInesperadaException;
import com.banco.pagos.exception.OperacionRechazadaException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Prueba la proteccion Retry + CircuitBreaker con las mismas reglas que config-repo (esperas cortas).
class CuentasClientTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CuentasClient cliente;
    private final AtomicInteger llamadas = new AtomicInteger();

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(6).minimumNumberOfCalls(6).failureRateThreshold(50)
                .recordExceptions(CuentasNoDisponibleException.class).build());
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3).waitDuration(Duration.ofMillis(1))
                .retryExceptions(CuentasNoDisponibleException.class).build());
        cliente = new CuentasClient(null, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void cuentasNoDisponible_seReintentaTresVecesYPropaga503() {
        CuentasNoDisponibleException ex = assertThrows(CuentasNoDisponibleException.class, () -> cliente.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new CuentasNoDisponibleException("servicio-cuentas no esta disponible", null);
        }));
        assertThat(llamadas).hasValue(3);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void rechazoDeNegocio409_noSeReintenta() {
        assertThrows(OperacionRechazadaException.class, () -> cliente.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new OperacionRechazadaException("Saldo insuficiente para realizar el debito");
        }));
        assertThat(llamadas).hasValue(1);
    }

    @Test
    void otro4xx_noSeReintentaNiAbreElCircuito() {
        for (int i = 0; i < 6; i++) {
            assertThrows(CuentasRespuestaInesperadaException.class, () -> cliente.ejecutar(() -> {
                llamadas.incrementAndGet();
                throw new CuentasRespuestaInesperadaException("servicio-cuentas respondio 403", null);
            }));
        }
        assertThat(llamadas).hasValue(6);
        assertThat(circuitBreakerRegistry.circuitBreaker(CuentasClient.INSTANCIA).getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void timeoutTransitorio_seRecuperaConElReintento() {
        String resultado = cliente.ejecutar(() -> {
            if (llamadas.incrementAndGet() == 1) {
                throw new CuentasNoDisponibleException("Read timed out", null);
            }
            return "ok";
        });
        assertThat(resultado).isEqualTo("ok");
        assertThat(llamadas).hasValue(2);
    }

    @Test
    void circuitoAbierto_fallbackResponde503DeInmediatoSinLlamar() {
        circuitBreakerRegistry.circuitBreaker(CuentasClient.INSTANCIA).transitionToOpenState();

        CuentasNoDisponibleException ex = assertThrows(CuentasNoDisponibleException.class,
                () -> cliente.ejecutar(() -> { llamadas.incrementAndGet(); return "ok"; }));

        assertThat(ex.getMessage()).isEqualTo("servicio-cuentas no esta disponible (circuito abierto)");
        assertThat(llamadas).hasValue(0);
    }

    @Test
    void dosPagosFallidos_abrenElCircuito() {
        for (int i = 0; i < 2; i++) {
            assertThrows(CuentasNoDisponibleException.class, () -> cliente.ejecutar(() -> {
                throw new CuentasNoDisponibleException("caido", null);
            }));
        }
        assertThat(circuitBreakerRegistry.circuitBreaker(CuentasClient.INSTANCIA).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }
}
