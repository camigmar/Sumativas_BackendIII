package com.banco.bffmovil.client;

import com.banco.bffmovil.dto.CuentaDTO;
import com.banco.bffmovil.dto.MovimientoDTO;
import com.banco.bffmovil.exception.CoreNoDisponibleException;
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

    @CircuitBreaker(name = "core", fallbackMethod = "existeCuentaFallback")
    public boolean existeCuenta(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/existe", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
    }

    private boolean existeCuentaFallback(Long cuentaId, Throwable t) {
        throw new CoreNoDisponibleException("No se pudo verificar la existencia de la cuenta " + cuentaId, t);
    }

    @CircuitBreaker(name = "core", fallbackMethod = "obtenerUltimosMovimientosFallback")
    public List<MovimientoDTO> obtenerUltimosMovimientos(Long cuentaId, int limite) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/movimientos?limite={limite}", cuentaId, limite)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MovimientoDTO>>() {
                });
    }

    private List<MovimientoDTO> obtenerUltimosMovimientosFallback(Long cuentaId, int limite, Throwable t) {
        throw new CoreNoDisponibleException("No se pudieron consultar los ultimos movimientos de la cuenta " + cuentaId, t);
    }
}
