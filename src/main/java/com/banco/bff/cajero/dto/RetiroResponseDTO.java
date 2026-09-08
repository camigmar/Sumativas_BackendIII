package com.banco.bff.cajero.dto;

public record RetiroResponseDTO(Long cuentaId, Double montoRetirado, Double saldoRestante) {
}
