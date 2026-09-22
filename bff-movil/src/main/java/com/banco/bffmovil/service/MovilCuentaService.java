package com.banco.bffmovil.service;

import com.banco.bffmovil.client.CoreClient;
import com.banco.bffmovil.dto.CuentaMovilDTO;
import com.banco.bffmovil.dto.MovimientoMovilDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovilCuentaService {

    private static final int LIMITE_MOVIMIENTOS = 10;

    private final CoreClient coreClient;

    public MovilCuentaService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public Optional<CuentaMovilDTO> obtenerDetalle(Long cuentaId) {
        return coreClient.obtenerCuenta(cuentaId).map(CuentaMovilDTO::desde);
    }

    public boolean existeCuenta(Long cuentaId) {
        return coreClient.existeCuenta(cuentaId);
    }

    public List<MovimientoMovilDTO> obtenerMovimientos(Long cuentaId) {
        return coreClient.obtenerUltimosMovimientos(cuentaId, LIMITE_MOVIMIENTOS)
                .stream()
                .map(MovimientoMovilDTO::desde)
                .toList();
    }
}
