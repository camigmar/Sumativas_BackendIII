package com.banco.bffweb.dto;

import java.time.LocalDate;

public record TransaccionDTO(Long id, LocalDate fecha, Double monto, String tipo) {
}
