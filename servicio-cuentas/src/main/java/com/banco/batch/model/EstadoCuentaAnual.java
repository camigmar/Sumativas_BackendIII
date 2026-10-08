package com.banco.batch.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estados_cuenta_anual")
public class EstadoCuentaAnual {

    @Id
    private Long cuentaId;
    private Double totalIngresos;
    private Double totalEgresos;
    private Double saldoNeto;
    private Integer cantidadMovimientos;

    public Long getCuentaId() {
        return cuentaId;
    }
    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }
    public Double getTotalIngresos() {
        return totalIngresos;
    }
    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }
    public Double getTotalEgresos() {
        return totalEgresos;
    }
    public void setTotalEgresos(Double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }
    public Double getSaldoNeto() {
        return saldoNeto;
    }
    public void setSaldoNeto(Double saldoNeto) {
        this.saldoNeto = saldoNeto;
    }
    public Integer getCantidadMovimientos() {
        return cantidadMovimientos;
    }
    public void setCantidadMovimientos(Integer cantidadMovimientos) {
        this.cantidadMovimientos = cantidadMovimientos;
    }
}
