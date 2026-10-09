package com.banco.core.client;

import com.banco.core.dto.ClienteDTO;
import com.banco.core.exception.ClienteNoEncontradoException;
import com.banco.core.exception.ClientesNoDisponibleException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClientesClient {

    private final RestClient restClient;

    public ClientesClient(RestClient clientesRestClient) {
        this.restClient = clientesRestClient;
    }

    public ClienteDTO obtenerCliente(Long clienteId) {
        try {
            return restClient.get()
                    .uri("/api/clientes/{id}", clienteId)
                    .retrieve()
                    .body(ClienteDTO.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ClienteNoEncontradoException("El cliente " + clienteId + " no existe");
            }
            throw new ClientesNoDisponibleException(
                    "servicio-clientes respondio " + ex.getStatusCode().value(), ex);
        } catch (RuntimeException ex) {
            // Conexion rechazada, timeout o sin instancias registradas en Eureka.
            throw new ClientesNoDisponibleException("servicio-clientes no esta disponible", ex);
        }
    }
}
