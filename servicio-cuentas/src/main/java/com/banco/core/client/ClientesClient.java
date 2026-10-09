package com.banco.core.client;

import com.banco.core.dto.ClienteDTO;
import com.banco.core.exception.ClienteNoEncontradoException;
import com.banco.core.exception.ClientesNoDisponibleException;
import com.banco.core.exception.ClientesRespuestaInesperadaException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class ClientesClient {

    static final String INSTANCIA = "clientes";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ClientesClient(RestClient clientesRestClient, CircuitBreakerRegistry circuitBreakerRegistry,
                          RetryRegistry retryRegistry) {
        this.restClient = clientesRestClient;
        // Configuracion de ambas instancias en config-repo (resilience4j.*.instances.clientes).
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCIA);
        this.retry = retryRegistry.retry(INSTANCIA);
    }

    public ClienteDTO obtenerCliente(Long clienteId) {
        return ejecutar(() -> consultar(clienteId));
    }

    // Retry por fuera del CircuitBreaker: cada intento cuenta para el circuito. Solo se reintenta
    // ClientesNoDisponibleException; con el circuito abierto se responde de inmediato, sin reintentar.
    <T> T ejecutar(Supplier<T> llamada) {
        Supplier<T> protegida = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, llamada));
        try {
            return protegida.get();
        } catch (CallNotPermittedException ex) {
            throw new ClientesNoDisponibleException("servicio-clientes no esta disponible (circuito abierto)", ex);
        }
    }

    private ClienteDTO consultar(Long clienteId) {
        try {
            return restClient.get()
                    .uri("/api/clientes/{id}", clienteId)
                    .retrieve()
                    .body(ClienteDTO.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ClienteNoEncontradoException("El cliente " + clienteId + " no existe");
            }
            if (ex.getStatusCode().is4xxClientError()) {
                // 4xx inesperado (por ejemplo 403): no se reintenta, reintentar no lo arregla.
                throw new ClientesRespuestaInesperadaException(
                        "servicio-clientes rechazo la consulta con " + ex.getStatusCode().value(), ex);
            }
            throw new ClientesNoDisponibleException(
                    "servicio-clientes respondio " + ex.getStatusCode().value(), ex);
        } catch (RuntimeException ex) {
            // Conexion rechazada, timeout, sin instancias en Eureka o sin token de auth-server.
            throw new ClientesNoDisponibleException("servicio-clientes no esta disponible", ex);
        }
    }
}
