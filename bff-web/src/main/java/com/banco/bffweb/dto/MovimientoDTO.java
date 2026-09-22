package com.banco.bffweb.dto;

import java.time.LocalDate;

public record MovimientoDTO(Long id, Long cuentaId, LocalDate fecha, String transaccion, Double monto,
                             String descripcion) {
}
