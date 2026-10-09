package com.banco.core.client;

import com.banco.core.exception.ClienteNoEncontradoException;
import com.banco.core.exception.ClientesNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Prueba la proteccion Retry + CircuitBreaker (mismas reglas que config-repo, esperas cortas).
class ClientesClientTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private ClientesClient cliente;
    private final AtomicInteger llamadas = new AtomicInteger();

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(6).minimumNumberOfCalls(6).failureRateThreshold(50)
                .recordExceptions(ClientesNoDisponibleException.class).build());
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3).waitDuration(Duration.ofMillis(1))
                .retryExceptions(ClientesNoDisponibleException.class).build());
        cliente = new ClientesClient(null, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void servicioNoDisponible_seReintentaYLuegoPropagaLaExcepcion() {
        assertThrows(ClientesNoDisponibleException.class, () -> cliente.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new ClientesNoDisponibleException("servicio-clientes no esta disponible", null);
        }));
        assertThat(llamadas).hasValue(3);
    }

    @Test
    void errorDeNegocio4xx_noSeReintenta() {
        assertThrows(ClienteNoEncontradoException.class, () -> cliente.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new ClienteNoEncontradoException("El cliente 9 no existe");
        }));
        assertThat(llamadas).hasValue(1);
    }

    @Test
    void falloTransitorio_seRecuperaEnElReintento() {
        String resultado = cliente.ejecutar(() -> {
            if (llamadas.incrementAndGet() < 2) {
                throw new ClientesNoDisponibleException("timeout", null);
            }
            return "ok";
        });
        assertThat(resultado).isEqualTo("ok");
        assertThat(llamadas).hasValue(2);
    }

    @Test
    void circuitoAbierto_fallbackRespondeNoDisponibleSinLlamarNiReintentar() {
        circuitBreakerRegistry.circuitBreaker(ClientesClient.INSTANCIA).transitionToOpenState();

        ClientesNoDisponibleException ex = assertThrows(ClientesNoDisponibleException.class,
                () -> cliente.ejecutar(() -> { llamadas.incrementAndGet(); return "ok"; }));

        assertThat(ex.getMessage()).contains("circuito abierto");
        assertThat(llamadas).hasValue(0);
    }

    @Test
    void fallosRepetidos_abrenElCircuito() {
        for (int i = 0; i < 2; i++) {
            assertThrows(ClientesNoDisponibleException.class, () -> cliente.ejecutar(() -> {
                throw new ClientesNoDisponibleException("caido", null);
            }));
        }
        assertThat(circuitBreakerRegistry.circuitBreaker(ClientesClient.INSTANCIA).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }
}
