package com.banco.bffweb.service;

import com.banco.bffweb.client.CoreClient;
import com.banco.bffweb.dto.CuentaWebDTO;
import com.banco.bffweb.dto.MovimientoDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WebCuentaService {

    private final CoreClient coreClient;

    public WebCuentaService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public Optional<CuentaWebDTO> obtenerDetalle(Long cuentaId) {
        return coreClient.obtenerCuenta(cuentaId)
                .map(cuenta -> {
                    var estado = coreClient.obtenerEstadoAnual(cuentaId).orElse(null);
                    return CuentaWebDTO.desde(cuenta, estado);
                });
    }

    public List<MovimientoDTO> obtenerMovimientos(Long cuentaId) {
        return coreClient.obtenerMovimientos(cuentaId);
    }
}
