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

    // Nullable: las filas anteriores a este campo quedan en null y se tratan como ACTIVA (ver getEstado).
    @Enumerated(EnumType.STRING)
    private EstadoCuenta estado = EstadoCuenta.ACTIVA;

    // Null en las cuentas cargadas por el batch, que no tienen cliente asociado.
    private Long clienteId;

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
    public EstadoCuenta getEstado() {
        return estado != null ? estado : EstadoCuenta.ACTIVA;
    }
    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }
    public boolean estaCerrada() {
        return getEstado() == EstadoCuenta.CERRADA;
    }
    public Long getClienteId() {
        return clienteId;
    }
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
}
