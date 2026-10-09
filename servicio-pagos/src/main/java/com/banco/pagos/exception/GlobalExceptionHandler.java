package com.banco.pagos.exception;

import com.banco.pagos.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Pago guardado como FALLIDO, COMPENSADO o REQUIERE_REVISION: 409 saldo insuficiente,
    // 404 cuenta inexistente, 503 servicio-cuentas no disponible, 500 si requiere revision.
    @ExceptionHandler(PagoFallidoException.class)
    public ResponseEntity<ErrorDTO> handlePagoFallido(PagoFallidoException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(CuentasException.class)
    public ResponseEntity<ErrorDTO> handleCuentas(CuentasException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> handlePagoNoEncontrado(PagoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(TransferenciaInvalidaException.class)
    public ResponseEntity<ErrorDTO> handleTransferenciaInvalida(TransferenciaInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorDTO(mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> handleCuerpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("El cuerpo de la solicitud no es valido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> handleParametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("El parametro " + ex.getParameterName() + " es obligatorio"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("El valor de " + ex.getName() + " no es valido"));
    }
}
