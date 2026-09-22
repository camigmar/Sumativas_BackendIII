package com.banco.bffweb.dto;

public record EstadoAnualDTO(Long cuentaId, Double totalIngresos, Double totalEgresos, Double saldoNeto,
                              Integer cantidadMovimientos) {
}
