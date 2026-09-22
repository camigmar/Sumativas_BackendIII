package com.banco.bffcajero.dto;

public record CuentaDTO(Long cuentaId, String nombre, Double saldo, Integer edad, String tipo, Double saldoFinal) {
}
