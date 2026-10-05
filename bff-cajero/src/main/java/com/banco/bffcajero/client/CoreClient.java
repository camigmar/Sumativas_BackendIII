package com.banco.bffcajero.client;

import com.banco.bffcajero.dto.CuentaDTO;
import com.banco.bffcajero.dto.ErrorDTO;
import com.banco.bffcajero.dto.RetiroRequestDTO;
import com.banco.bffcajero.dto.RetiroResponseDTO;
import com.banco.bffcajero.exception.CoreErrorException;
import com.banco.bffcajero.exception.CoreNoDisponibleException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

    @CircuitBreaker(name = "core", fallbackMethod = "retirarFallback")
    public RetiroResponseDTO retirar(Long cuentaId, RetiroRequestDTO request) {
        try {
            return restClient.post()
                    .uri("/api/core/cuentas/{cuentaId}/retiro", cuentaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RetiroResponseDTO.class);
        } catch (RestClientResponseException ex) {
            ErrorDTO error = ex.getResponseBodyAs(ErrorDTO.class);
            String mensaje = error != null ? error.mensaje() : ex.getMessage();
            throw new CoreErrorException(ex.getStatusCode().value(), mensaje);
        }
    }

    // ignore-exceptions solo evita que CoreErrorException cuente como falla del circuito,
    // pero el fallback se ejecuta igual. Resilience4j elige el fallback con el tipo de
    // excepcion mas especifico, asi que este relanza el error de negocio (409, 404...) intacto.
    private RetiroResponseDTO retirarFallback(Long cuentaId, RetiroRequestDTO request, CoreErrorException ex) {
        throw ex;
    }

    private RetiroResponseDTO retirarFallback(Long cuentaId, RetiroRequestDTO request, Throwable t) {
        throw new CoreNoDisponibleException("No se pudo realizar el retiro en la cuenta " + cuentaId, t);
    }
}
