package com.banco.bffweb.client;

import com.banco.bffweb.dto.CuentaDTO;
import com.banco.bffweb.dto.EstadoAnualDTO;
import com.banco.bffweb.dto.MovimientoDTO;
import com.banco.bffweb.dto.TransaccionDTO;
import com.banco.bffweb.exception.CoreNoDisponibleException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class CoreClient {

    private final RestClient restClient;

    public CoreClient(RestClient coreRestClient) {
        this.restClient = coreRestClient;
    }

    @CircuitBreaker(name = "core", fallbackMethod = "obtenerCuentaFallback")
    public Optional<CuentaDTO> obtenerCuenta(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(CuentaDTO.class))
                        : Optional.empty());
    }

    private Optional<CuentaDTO> obtenerCuentaFallback(Long cuentaId, Throwable t) {
        throw new CoreNoDisponibleException("No se pudo consultar la cuenta " + cuentaId, t);
    }

    @CircuitBreaker(name = "core", fallbackMethod = "obtenerEstadoAnualFallback")
    public Optional<EstadoAnualDTO> obtenerEstadoAnual(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/estado-anual", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(EstadoAnualDTO.class))
                        : Optional.empty());
    }

    private Optional<EstadoAnualDTO> obtenerEstadoAnualFallback(Long cuentaId, Throwable t) {
        throw new CoreNoDisponibleException("No se pudo consultar el estado anual de la cuenta " + cuentaId, t);
    }

    @CircuitBreaker(name = "core", fallbackMethod = "obtenerMovimientosFallback")
    public List<MovimientoDTO> obtenerMovimientos(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/movimientos", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MovimientoDTO>>() {
                });
    }

    private List<MovimientoDTO> obtenerMovimientosFallback(Long cuentaId, Throwable t) {
        throw new CoreNoDisponibleException("No se pudieron consultar los movimientos de la cuenta " + cuentaId, t);
    }

    @CircuitBreaker(name = "core", fallbackMethod = "listarTransaccionesFallback")
    public List<TransaccionDTO> listarTransacciones() {
        return restClient.get()
                .uri("/api/core/transacciones")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionDTO>>() {
                });
    }

    private List<TransaccionDTO> listarTransaccionesFallback(Throwable t) {
        throw new CoreNoDisponibleException("No se pudieron listar las transacciones", t);
    }

    @CircuitBreaker(name = "core", fallbackMethod = "obtenerTransaccionFallback")
    public Optional<TransaccionDTO> obtenerTransaccion(Long id) {
        return restClient.get()
                .uri("/api/core/transacciones/{id}", id)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(TransaccionDTO.class))
                        : Optional.empty());
    }

    private Optional<TransaccionDTO> obtenerTransaccionFallback(Long id, Throwable t) {
        throw new CoreNoDisponibleException("No se pudo consultar la transaccion " + id, t);
    }
}
