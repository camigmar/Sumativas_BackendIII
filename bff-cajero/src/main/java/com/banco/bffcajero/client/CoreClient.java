package com.banco.bffcajero.client;

import com.banco.bffcajero.dto.CuentaDTO;
import com.banco.bffcajero.dto.ErrorDTO;
import com.banco.bffcajero.dto.RetiroRequestDTO;
import com.banco.bffcajero.dto.RetiroResponseDTO;
import com.banco.bffcajero.dto.ValidarCredencialesRequest;
import com.banco.bffcajero.dto.ValidarCredencialesResponse;
import com.banco.bffcajero.exception.CoreErrorException;
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
}
