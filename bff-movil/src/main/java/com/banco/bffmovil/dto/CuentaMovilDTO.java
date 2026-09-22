package com.banco.bffmovil.dto;

public record CuentaMovilDTO(Long cuentaId, String nombre, Double saldo, String tipo) {

    public static CuentaMovilDTO desde(CuentaDTO cuenta) {
        return new CuentaMovilDTO(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.saldo(),
                cuenta.tipo()
        );
    }
}
