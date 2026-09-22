package com.banco.bffcajero.service;

import com.banco.bffcajero.client.CoreClient;
import com.banco.bffcajero.dto.RetiroRequestDTO;
import com.banco.bffcajero.dto.RetiroResponseDTO;
import com.banco.bffcajero.dto.SaldoDTO;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CajeroService {

    private final CoreClient coreClient;

    public CajeroService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public Optional<SaldoDTO> obtenerSaldo(Long cuentaId) {
        return coreClient.obtenerCuenta(cuentaId)
                .map(cuenta -> new SaldoDTO(cuenta.cuentaId(), cuenta.saldo()));
    }

    public boolean existeCuenta(Long cuentaId) {
        return coreClient.existeCuenta(cuentaId);
    }

    public RetiroResponseDTO retirar(Long cuentaId, RetiroRequestDTO request) {
        return coreClient.retirar(cuentaId, request);
    }
}
