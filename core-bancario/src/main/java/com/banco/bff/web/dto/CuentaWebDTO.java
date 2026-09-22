package com.banco.bff.web.dto;
import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuentaAnual;
public record CuentaWebDTO(
        Long cuentaId, String nombre, Double saldo, Integer edad, String tipo, Double saldoFinal,
        Double totalIngresos, Double totalEgresos, Double saldoNeto, Integer cantidadMovimientos
) {
    public static CuentaWebDTO desde(CuentaInteres cuenta, EstadoCuentaAnual estado) {
        return new CuentaWebDTO(
                cuenta.getCuentaId(), cuenta.getNombre(), cuenta.getSaldo(), cuenta.getEdad(),
                cuenta.getTipo(), cuenta.getSaldoFinal(),
                estado != null ? estado.getTotalIngresos() : null,
                estado != null ? estado.getTotalEgresos() : null,
                estado != null ? estado.getSaldoNeto() : null,
                estado != null ? estado.getCantidadMovimientos() : null
        );
    }
}
