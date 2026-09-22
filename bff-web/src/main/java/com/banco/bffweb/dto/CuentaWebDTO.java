package com.banco.bffweb.dto;

public record CuentaWebDTO(
        Long cuentaId, String nombre, Double saldo, Integer edad, String tipo, Double saldoFinal,
        Double totalIngresos, Double totalEgresos, Double saldoNeto, Integer cantidadMovimientos
) {
    public static CuentaWebDTO desde(CuentaDTO cuenta, EstadoAnualDTO estado) {
        return new CuentaWebDTO(
                cuenta.cuentaId(), cuenta.nombre(), cuenta.saldo(), cuenta.edad(),
                cuenta.tipo(), cuenta.saldoFinal(),
                estado != null ? estado.totalIngresos() : null,
                estado != null ? estado.totalEgresos() : null,
                estado != null ? estado.saldoNeto() : null,
                estado != null ? estado.cantidadMovimientos() : null
        );
    }
}
