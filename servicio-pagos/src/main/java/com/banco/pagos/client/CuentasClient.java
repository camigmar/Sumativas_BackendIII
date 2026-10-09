package com.banco.pagos.client;

import com.banco.pagos.dto.MovimientoCuentaRequestDTO;
import com.banco.pagos.dto.MovimientoCuentaResponseDTO;
import com.banco.pagos.exception.CuentaNoEncontradaException;
import com.banco.pagos.exception.CuentasException;
import com.banco.pagos.exception.CuentasNoDisponibleException;
import com.banco.pagos.exception.MontoInvalidoException;
import com.banco.pagos.exception.SaldoInsuficienteException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CuentasClient {

    private final RestClient restClient;

    public CuentasClient(RestClient cuentasRestClient) {
        this.restClient = cuentasRestClient;
    }

    public MovimientoCuentaResponseDTO debitar(Long cuentaId, Double monto) {
        return mover("debito", cuentaId, monto);
    }

    public MovimientoCuentaResponseDTO acreditar(Long cuentaId, Double monto) {
        return mover("credito", cuentaId, monto);
    }

    private MovimientoCuentaResponseDTO mover(String operacion, Long cuentaId, Double monto) {
        try {
            return restClient.post()
                    .uri("/api/core/cuentas/{cuentaId}/{operacion}", cuentaId, operacion)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MovimientoCuentaRequestDTO(monto))
                    .retrieve()
                    .body(MovimientoCuentaResponseDTO.class);
        } catch (RestClientResponseException ex) {
            throw traducir(ex, operacion, cuentaId);
        } catch (RuntimeException ex) {
            // Conexion rechazada, timeout o sin instancias registradas en Eureka.
            throw new CuentasNoDisponibleException("servicio-cuentas no esta disponible", ex);
        }
    }

    private CuentasException traducir(RestClientResponseException ex, String operacion, Long cuentaId) {
        return switch (ex.getStatusCode().value()) {
            case 409 -> new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + cuentaId);
            case 404 -> new CuentaNoEncontradaException("La cuenta " + cuentaId + " no existe");
            case 400 -> new MontoInvalidoException("servicio-cuentas rechazo el monto del " + operacion);
            default -> new CuentasNoDisponibleException(
                    "servicio-cuentas respondio " + ex.getStatusCode().value() + " al " + operacion, ex);
        };
    }
}
