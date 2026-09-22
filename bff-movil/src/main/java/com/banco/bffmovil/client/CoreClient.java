package com.banco.bffmovil.client;

import com.banco.bffmovil.dto.CuentaDTO;
import com.banco.bffmovil.dto.MovimientoDTO;
import com.banco.bffmovil.dto.ValidarCredencialesRequest;
import com.banco.bffmovil.dto.ValidarCredencialesResponse;
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

    public boolean existeCuenta(Long cuentaId) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/existe", cuentaId)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
    }

    public List<MovimientoDTO> obtenerUltimosMovimientos(Long cuentaId, int limite) {
        return restClient.get()
                .uri("/api/core/cuentas/{cuentaId}/movimientos?limite={limite}", cuentaId, limite)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MovimientoDTO>>() {
                });
    }
}
