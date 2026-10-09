package com.banco.core.controller;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.model.MovimientoAnual;
import com.banco.core.dto.ActualizarCuentaRequestDTO;
import com.banco.core.dto.AperturaCuentaRequestDTO;
import com.banco.core.dto.ErrorDTO;
import com.banco.core.dto.MovimientoRequestDTO;
import com.banco.core.dto.MovimientoResponseDTO;
import com.banco.core.dto.RetiroRequestDTO;
import com.banco.core.dto.RetiroResponseDTO;
import com.banco.core.exception.CierreNoPermitidoException;
import com.banco.core.exception.ClienteInactivoException;
import com.banco.core.exception.ClienteNoEncontradoException;
import com.banco.core.exception.ClientesNoDisponibleException;
import com.banco.core.exception.CuentaCerradaException;
import com.banco.core.exception.CuentaNoEncontradaException;
import com.banco.core.exception.MontoInvalidoException;
import com.banco.core.exception.SaldoInsuficienteException;
import com.banco.core.service.CuentaService;
import com.banco.core.service.MovimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/core/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;

    public CuentaController(CuentaService cuentaService, MovimientoService movimientoService) {
        this.cuentaService = cuentaService;
        this.movimientoService = movimientoService;
    }

    @PostMapping
    public ResponseEntity<CuentaInteres> abrir(@Valid @RequestBody AperturaCuentaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.abrirCuenta(request));
    }

    @GetMapping
    public List<CuentaInteres> listarPorCliente(@RequestParam Long clienteId) {
        return cuentaService.listarPorCliente(clienteId);
    }

    @PutMapping("/{cuentaId}")
    public CuentaInteres actualizar(@PathVariable Long cuentaId, @Valid @RequestBody ActualizarCuentaRequestDTO request) {
        return cuentaService.actualizarCuenta(cuentaId, request);
    }

    @PostMapping("/{cuentaId}/cierre")
    public CuentaInteres cerrar(@PathVariable Long cuentaId) {
        return cuentaService.cerrarCuenta(cuentaId);
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaInteres> obtener(@PathVariable Long cuentaId) {
        return cuentaService.obtenerCuenta(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/existe")
    public ResponseEntity<Void> existe(@PathVariable Long cuentaId) {
        return cuentaService.existeCuenta(cuentaId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{cuentaId}/estado-anual")
    public ResponseEntity<EstadoCuentaAnual> estadoAnual(@PathVariable Long cuentaId) {
        return cuentaService.obtenerEstadoAnual(cuentaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public ResponseEntity<List<MovimientoAnual>> movimientos(@PathVariable Long cuentaId,
                                                               @RequestParam(required = false) Integer limite) {
        List<MovimientoAnual> movimientos = limite != null
                ? movimientoService.obtenerUltimosMovimientos(cuentaId, limite)
                : movimientoService.obtenerMovimientos(cuentaId);
        return ResponseEntity.ok(movimientos);
    }

    @PostMapping("/{cuentaId}/retiro")
    public ResponseEntity<RetiroResponseDTO> retiro(@PathVariable Long cuentaId, @RequestBody RetiroRequestDTO request) {
        double nuevoSaldo = cuentaService.retirar(cuentaId, request.monto());
        return ResponseEntity.ok(new RetiroResponseDTO(cuentaId, request.monto(), nuevoSaldo));
    }

    @PostMapping("/{cuentaId}/debito")
    public ResponseEntity<MovimientoResponseDTO> debito(@PathVariable Long cuentaId, @RequestBody MovimientoRequestDTO request) {
        double nuevoSaldo = cuentaService.debitar(cuentaId, request.monto());
        return ResponseEntity.ok(new MovimientoResponseDTO(cuentaId, request.monto(), nuevoSaldo));
    }

    @PostMapping("/{cuentaId}/credito")
    public ResponseEntity<MovimientoResponseDTO> credito(@PathVariable Long cuentaId, @RequestBody MovimientoRequestDTO request) {
        double nuevoSaldo = cuentaService.acreditar(cuentaId, request.monto());
        return ResponseEntity.ok(new MovimientoResponseDTO(cuentaId, request.monto(), nuevoSaldo));
    }

    @ExceptionHandler(MontoInvalidoException.class)
    public ResponseEntity<ErrorDTO> handleMontoInvalido(MontoInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ErrorDTO> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorDTO> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler({CuentaCerradaException.class, CierreNoPermitidoException.class, ClienteInactivoException.class})
    public ResponseEntity<ErrorDTO> handleConflicto(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(ClientesNoDisponibleException.class)
    public ResponseEntity<ErrorDTO> handleClientesNoDisponible(ClientesNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorDTO(mensaje));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> handleParametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("El parametro " + ex.getParameterName() + " es obligatorio"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> handleCuerpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("El cuerpo de la solicitud no es valido"));
    }
}
