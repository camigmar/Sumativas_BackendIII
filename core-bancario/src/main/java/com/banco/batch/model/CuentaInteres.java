package com.banco.batch.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cuentas_interes")
public class CuentaInteres {

    @Id
    private Long cuentaId;
    private String nombre;
    private Double saldo;
    private Integer edad;
    private String tipo;
    private Double saldoFinal;

    public Long getCuentaId() {
        return cuentaId;
    }
    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public Double getSaldo() {
        return saldo;
    }
    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }
    public Integer getEdad() {
        return edad;
    }
    public void setEdad(Integer edad) {
        this.edad = edad;
    }
    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public Double getSaldoFinal() {
        return saldoFinal;
    }
    public void setSaldoFinal(Double saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}
