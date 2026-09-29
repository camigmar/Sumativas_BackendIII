package com.banco.bffmovil.exception;

import com.banco.bffmovil.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoreNoDisponibleException.class)
    public ResponseEntity<ErrorDTO> handleCoreNoDisponible(CoreNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorDTO("El servicio no está disponible en este momento. Intenta más tarde."));
    }
}
