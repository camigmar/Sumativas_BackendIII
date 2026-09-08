package com.banco.bff.movil;

import com.banco.batch.model.CuentaInteres;

public record CuentaMovilDTO(Long cuentaId, String nombre, Double saldo, String tipo) {

    public static CuentaMovilDTO fromCuentaInteres(CuentaInteres cuentaInteres) {
        return new CuentaMovilDTO(
                cuentaInteres.getCuentaId(),
                cuentaInteres.getNombre(),
                cuentaInteres.getSaldo(),
                cuentaInteres.getTipo()
        );
    }
}
