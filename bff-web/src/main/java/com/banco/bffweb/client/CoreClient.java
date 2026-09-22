package com.banco.bffweb.client;

import com.banco.bffweb.dto.CuentaDTO;
import com.banco.bffweb.dto.EstadoAnualDTO;
import com.banco.bffweb.dto.MovimientoDTO;
import com.banco.bffweb.dto.TransaccionDTO;
import com.banco.bffweb.dto.ValidarCredencialesRequest;
import com.banco.bffweb.dto.ValidarCredencialesResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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

    public Optional<String> validarCredenciales(String username, String password) {
        return restClient.post()
                .uri("/api/core/auth/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ValidarCredencialesRequest(username, password))
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(ValidarCredencialesResponse.class).rol())
                        : Optional.empty());
    }

    public Optional<CuentaDTO> obtenerCuenta(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(CuentaDTO.class))
                        : Optional.empty());
    }

    public Optional<EstadoAnualDTO> obtenerEstadoAnual(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/estado-anual", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(EstadoAnualDTO.class))
                        : Optional.empty());
    }

    public List<MovimientoDTO> obtenerMovimientos(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/movimientos", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MovimientoDTO>>() {
                });
    }

    public List<TransaccionDTO> listarTransacciones() {
        return restClient.get()
                .uri("/api/core/transacciones")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionDTO>>() {
                });
    }

    public Optional<TransaccionDTO> obtenerTransaccion(Long id) {
        return restClient.get()
                .uri("/api/core/transacciones/{id}", id)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Optional.of(response.bodyTo(TransaccionDTO.class))
                        : Optional.empty());
    }
}
