package com.banco.batch.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_anuales")
public class MovimientoAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private Double monto;
    private String descripcion;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getCuentaId() {
        return cuentaId;
    }
    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }
    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
    public String getTransaccion() {
        return transaccion;
    }
    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }
    public Double getMonto() {
        return monto;
    }
    public void setMonto(Double monto) {
        this.monto = monto;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
