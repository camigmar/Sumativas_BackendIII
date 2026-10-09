package com.banco.auditoria.exception;

import com.banco.auditoria.dto.ErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Las excepciones de Spring MVC (404 de ruta inexistente, 405, etc.) traen su propio status;
    // cualquier otra es un error interno. Todas responden con el mismo formato { "mensaje": ... }.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleError(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String mensaje = status.value() == 404 ? "Recurso no encontrado" : errorResponse.getBody().getDetail();
            return ResponseEntity.status(status).body(new ErrorDTO(mensaje));
        }
        logger.error("Error al consultar la auditoria", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO("Error interno al consultar la auditoria"));
    }
}
