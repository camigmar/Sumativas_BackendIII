package com.banco.core.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Debito o credito ya aplicado con una Idempotency-Key. Si la misma clave llega otra vez (reintento de
// servicio-pagos tras un timeout, o una peticion duplicada), se devuelve este resultado sin mover saldo.
@Entity
@Table(name = "movimientos_idempotentes")
public class MovimientoIdempotente {

    @Id
    @Column(length = 100)
    private String clave;

    @Column(nullable = false)
    private Long cuentaId;

    // "debito" o "credito".
    @Column(nullable = false)
    private String operacion;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private Double nuevoSaldo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public String getClave() {
        return clave;
    }
    public void setClave(String clave) {
        this.clave = clave;
    }
    public Long getCuentaId() {
        return cuentaId;
    }
    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }
    public String getOperacion() {
        return operacion;
    }
    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }
    public Double getMonto() {
        return monto;
    }
    public void setMonto(Double monto) {
        this.monto = monto;
    }
    public Double getNuevoSaldo() {
        return nuevoSaldo;
    }
    public void setNuevoSaldo(Double nuevoSaldo) {
        this.nuevoSaldo = nuevoSaldo;
    }
    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
